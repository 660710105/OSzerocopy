package zerocopy.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;

import zerocopy.ioutils.Jio;
import zerocopy.ioutils.Protocol;

public class HandlerClient implements Runnable {
    private SocketChannel clientChannel;
    private File fileDir;
    private SocketAddress clientAddr;
    private Jio jio; // เพิ่ม

    HandlerClient(Socket client, File fileDir) throws IOException {
        this.clientChannel = client.getChannel(); // <--- เอา Channel มา
        if (this.clientChannel == null) {
            throw new IOException("Failed to get SocketChannel from socket.");
        }
        this.clientChannel.configureBlocking(true); // <--- ตั้งเป็น Blocking
        this.fileDir = fileDir;
        this.clientAddr = client.getRemoteSocketAddress();
        this.jio = new Jio();
        System.out.println(" >> Client " + clientAddr + " has connected.");
    }

    @Override
    public void run() {
        try {
            // ลูปหลัก: รอรับ "รหัสคำสั่ง" (Command Code)
            while (clientChannel.isOpen()) {
                int command = Protocol.readInt(clientChannel);

                switch (command) {
                    case Protocol.REQ_FILE_LIST:
                        System.out.println(" >> Client " + clientAddr + " requested file list.");
                        handleSendFileList();
                        break;
                    case Protocol.REQ_MAIN:
                        System.out.println(" >> Client " + clientAddr + " sent MAIN request.");
                        handleMainRequest();
                        break;
                    case Protocol.REQ_DATA_SINGLE: // (โหมด 0 หรือ 1)
                        System.out.println(" >> Client " + clientAddr + " opened DATA connection (Single).");
                        handleDataRequestSingle();
                        break;
                    case Protocol.REQ_DATA_PARTIAL: // (โหมด 2 หรือ 3)
                        System.out.println(" >> Client " + clientAddr + " opened DATA connection (Partial).");
                        handlePartialRequest();
                        break;
                    case Protocol.REQ_COMPLETE:
                        System.out.println(" >> Client " + clientAddr + " (Control Plane) finished.");
                        clientChannel.close(); // ปิด Control Plane
                        break;
                    default:
                        System.err.println("Unknown command: " + command);
                }
            }
        } catch (IOException e) {
            System.err.println(" >> Client " + clientAddr + " disconnected.");
        } finally {
            try {
                if (clientChannel.isOpen()) clientChannel.close();
            } catch (IOException e) { /* ignore */ }
        }
    }

    // (Handler 1) ส่งรายชื่อไฟล์
    private void handleSendFileList() throws IOException {
        File[] files = fileDir.listFiles();
        List<String> listFileName = new ArrayList<>();
        for (File f : files) {
            if (f.isFile()) listFileName.add(f.getName());
        }
        
        Protocol.writeInt(clientChannel, listFileName.size()); // 1. ส่งจำนวนไฟล์
        for (String name : listFileName) {
            Protocol.writeString(clientChannel, name); // 2. ส่งชื่อไฟล์
        }
    }

    // (Handler 2) รับคำขอหลัก (ส่งกลับแค่ขนาดไฟล์)
    private void handleMainRequest() throws IOException {
        String filename = Protocol.readString(clientChannel);
        int modeIdx = Protocol.readInt(clientChannel);
        int nthread = Protocol.readInt(clientChannel);
        
        System.out.println(" >> Main request: " + filename + " (Mode: " + modeIdx + ", Threads: " + nthread + ")");
        
        File fileToSend = new File(fileDir, filename);
        long fileSize = -1;
        if (fileToSend.exists() && fileToSend.isFile()) {
            fileSize = fileToSend.length();
        }
        
        Protocol.writeLong(clientChannel, fileSize); // ส่งขนาดไฟล์กลับ (หรือ -1 ถ้าไม่เจอ)
        // จบหน้าที่ของ Control Plane (สำหรับคำขอนี้)
    }

    // (Handler 3) ส่งไฟล์เต็ม (โหมด 0 หรือ 1)
    private void handleDataRequestSingle() throws IOException {
        String filename = Protocol.readString(clientChannel);
        boolean isZeroCopy = (Protocol.readInt(clientChannel) == 1);
        
        File fileToSend = new File(fileDir, filename);
        if (!fileToSend.exists()) {
            System.err.println("Data request for non-existent file: " + filename);
            return;
        }

        try (FileInputStream fis = new FileInputStream(fileToSend);
             FileChannel fileChannel = fis.getChannel()) {
            
            if (isZeroCopy) {
                // --- 🚀 Server-Side Zero-Copy (Mode 1) ---
                System.out.println(" >> Server sending (Zero-Copy)...");
                jio.zeroCopyTransfer(fileToSend, fileChannel, clientChannel);
            } else {
                // --- Server-Side Copy (Mode 0) ---
                System.out.println(" >> Server sending (Copy)...");
                OutputStream os = Channels.newOutputStream(clientChannel);
                jio.copyTransfer(fileToSend, fis, os);
            }
        }
        clientChannel.close(); // ปิด Data Plane
    }

    // (Handler 4) ส่งไฟล์ย่อย (โหมด 2 หรือ 3)
    private void handlePartialRequest() throws IOException {
        String filename = Protocol.readString(clientChannel);
        long startByte = Protocol.readLong(clientChannel);
        long endByte = Protocol.readLong(clientChannel);
        String mode = Protocol.readString(clientChannel);
        
        File fileToSend = new File(fileDir, filename);
        if (!fileToSend.exists()) {
            System.err.println("Partial request for non-existent file: " + filename);
            return;
        }

        long partSize = (endByte - startByte) + 1;
        Protocol.writeLong(clientChannel, partSize); // ส่งขนาด "ส่วน" นี้กลับไป

        try (FileInputStream fis = new FileInputStream(fileToSend);
             FileChannel fileChannel = fis.getChannel()) {
            
            if (mode.equals("Zero-Copy-MultiThreads")) {
                // --- 🚀 Server-Side Zero-Copy (Mode 3) ---
                System.out.println(" >> Server sending partial (Zero-Copy)...");
                jio.partialZeroCopyTransfer(fileChannel, clientChannel, startByte, partSize);
            } else {
                // --- Server-Side Copy (Mode 2) ---
                System.out.println(" >> Server sending partial (Copy)...");
                fis.skip(startByte);
                jio.partialCopyTransfer(fis, clientChannel, partSize);
            }
        }
        clientChannel.close(); // ปิด Data Plane
    }
}