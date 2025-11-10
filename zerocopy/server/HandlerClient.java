package zerocopy.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel; // เพิ่ม
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;

import zerocopy.ioutils.Jio;

public class HandlerClient implements Runnable {
    private Socket client;
    private File fileDir;
    private SocketAddress clientAddr;
    private Jio jio; // เพิ่ม

    HandlerClient(Socket client, File fileDir) {
        this.client = client;
        this.fileDir = fileDir;
        this.jio = new Jio(); // เพิ่ม
    }

    @Override
    public void run() {
        try {
            client.setKeepAlive(true);
            clientAddr = client.getRemoteSocketAddress();
            
            ObjectOutputStream oout = new ObjectOutputStream(client.getOutputStream());
            ObjectInputStream oin = new ObjectInputStream(client.getInputStream());

            // --- ส่วนที่ 1: อ่าน Request Type เพื่อแยกว่าเป็น Client หลัก หรือ เธรดลูก ---
            Object requestType = oin.readObject();
            if (!(requestType instanceof String)) {
                System.err.println("Invalid request from " + clientAddr);
                return;
            }

            String type = (String) requestType;
            if (type.equals("MAIN_REQUEST")) {
                System.out.println(" >> Client (Main) " + clientAddr + " has connected.");
                handleMainRequest(oout, oin);
            } else if (type.equals("PARTIAL_REQUEST")) {
                System.out.println(" >> Client (Worker) " + clientAddr + " has connected.");
                handlePartialRequest(oout, oin);
            } else {
                System.err.println("Unknown request type: " + type);
            }
            // ----------------------------------------------------------------

        } catch (Exception e) {
            // e.printStackTrace(); // ปิดไว้
            System.err.println(" >> Client " + clientAddr + " disconnected abruptly.");
        } finally {
            try {
                if (client != null && !client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * จัดการ Client (หลัก) ที่สั่งงาน
     */
    private void handleMainRequest(ObjectOutputStream oout, ObjectInputStream oin) throws IOException, ClassNotFoundException {
        // 1. ส่งรายชื่อไฟล์
        sendFilenameList(oout, fileDir);

        // 2. รับชื่อไฟล์และโหมด
        Object name = oin.readObject();
        if (!(name instanceof String)) {
            System.err.println("Error: expected filename string");
            return;
        }
        String filename = (String) name;
        int modeIdx = oin.readInt();
        String[] modes = { "Copy", "Zero-Copy", "Copy-MultiThreads", "Zero-Copy-MultiThreads" };
        String mode = modes[modeIdx];

        // 3. รับจำนวนเธรด (ถ้ามี)
        int nthread = 1;
        if (modeIdx == 2 || modeIdx == 3) {
            nthread = oin.readInt();
        }

        if (nthread == 1 && modeIdx == 2) {
            mode = "Copy"; // เปลี่ยนกลับไปใช้โหมด 0
            System.out.println(" >> Server Optimizing: 1 thread requested, falling back to 'Copy' (Mode 0)");
        }
        if (nthread == 1 && modeIdx == 3) {
            mode = "Zero-Copy"; // เปลี่ยนกลับไปใช้โหมด 1
            System.out.println(" >> Server Optimizing: 1 thread requested, falling back to 'Zero-Copy' (Mode 1)");
        }

        System.out.println(" >> Client " + clientAddr + " requested: " + filename + " (Mode: " + mode + ", Threads: " + nthread + ")");

        File fileToSend = new File(fileDir, filename);
        if (!fileToSend.exists() || !fileToSend.isFile()) {
            System.err.println("File not found: " + filename);
            oout.writeLong(-1); // ส่ง -1 บอกว่าหาไฟล์ไม่เจอ
            oout.flush();
            return;
        }

        long fileSize = fileToSend.length();
        oout.writeLong(fileSize); // 4. ส่งขนาดไฟล์
        oout.flush();
        
        OutputStream os = client.getOutputStream();
        FileInputStream fis = new FileInputStream(fileToSend);

        // 5. ดำเนินการส่งไฟล์ (เฉพาะโหมด 0 และ 1)
        switch (mode) {
            case "Copy":
                jio.copyTransfer(fileToSend, fis, os);
                break;
            case "Zero-Copy":
                FileChannel fileChannel = fis.getChannel();
                WritableByteChannel wbc = Channels.newChannel(os);
                jio.zeroCopyTransfer(fileToSend, fileChannel, wbc);
                fileChannel.close();
                break;
            case "Copy-MultiThreads":
            case "Zero-Copy-MultiThreads":
                // สำหรับโหมด Multi-thread เธรดหลักนี้ "ไม่ต้องทำอะไร"
                // แค่รอให้เธรดลูก (Worker) มาเชื่อมต่อและดึงไฟล์ส่วนย่อยไปเอง
                // เธรดหลักนี้จะรอสัญญาณ "complete" จาก Client
                System.out.println(" >> Main handler is waiting for worker threads to complete...");
                break;
            default:
                jio.copyTransfer(fileToSend, fis, os);
                break;
        }
        
        fis.close();

        // 6. รอสัญญาณ "เสร็จสิ้น" จาก Client (หลัก)
        boolean complete = oin.readBoolean();
        if (complete) {
            System.out.println(" >> Main handler task complete for " + clientAddr);
        }
    }
    
    /**
     * จัดการ Client (เธรดลูก) ที่มาขอไฟล์ส่วนย่อย
     */
    private void handlePartialRequest(ObjectOutputStream oout, ObjectInputStream oin) throws IOException, ClassNotFoundException {
        // 1. รับคำขอ (ชื่อไฟล์, ไบต์เริ่มต้น, ไบต์สิ้นสุด, โหมด)
        String filename = (String) oin.readObject();
        long startByte = oin.readLong();
        long endByte = oin.readLong();
        String mode = (String) oin.readObject(); // รับโหมด (Copy-Multi หรือ Zero-Copy-Multi)

        File fileToSend = new File(fileDir, filename);
        if (!fileToSend.exists()) {
            System.err.println("Worker " + clientAddr + " requested non-existent file: " + filename);
            oout.writeLong(-1); // ส่ง -1
            oout.flush();
            return;
        }
        
        long partSize = (endByte - startByte) + 1;
        System.out.println(" >> Worker " + clientAddr + " requesting " + filename + " (Bytes: " + startByte + " to " + endByte + ", Size: " + partSize + " bytes, Mode: " + mode + ")");

        oout.writeLong(partSize); // 2. ส่งขนาดของ "ส่วน" นี้กลับไป
        oout.flush();

        FileInputStream fis = new FileInputStream(fileToSend);
        OutputStream os = client.getOutputStream();
        
        // 3. เลือกวิธีการส่งไฟล์ "ส่วนย่อย"
        if (mode.equals("Copy-MultiThreads")) {
            fis.skip(startByte); // ข้ามไปยังจุดเริ่มต้น
            jio.partialCopyTransfer(fis, os, partSize); // ส่งแบบ Copy
            
        } else if (mode.equals("Zero-Copy-MultiThreads")) {
            FileChannel fileChannel = fis.getChannel();
            WritableByteChannel wbc = Channels.newChannel(os);
            // ส่งแบบ Zero-Copy (เริ่มที่ startByte, ส่งจำนวน partSize)
            jio.partialZeroCopyTransfer(fileChannel, wbc, startByte, partSize); 
            fileChannel.close();
        }
        
        fis.close();
        System.out.println(" >> Worker " + clientAddr + " finished.");
    }

    // (sendFilenameList ไม่เปลี่ยนแปลง)
    private void sendFilenameList(ObjectOutputStream oout, File file) {
        try {
            File[] files = file.listFiles();
            List<String> listFileName = new ArrayList<>();
            for (File f : files) {
                if (f instanceof File)
                    listFileName.add(f.getName());
            }
            oout.writeObject(listFileName);
            oout.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}