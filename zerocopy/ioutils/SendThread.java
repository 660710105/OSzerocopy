package zerocopy.ioutils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

import zerocopy.ioutils.Protocol;

public class SendThread implements Runnable {
    private String host, filename, mode, partFile;
    private int port;
    private long startByte, endByte;

    public SendThread(String filename, String host, int port, long startByte, long endByte, String partFile, String mode) {
        this.filename = filename;
        this.host = host;
        this.port = port;
        this.startByte = startByte;
        this.endByte = endByte;
        this.partFile = partFile;
        this.mode = mode;
    }

    @Override
    public void run() {
        // เปิด "Data Plane" (Socket ใหม่)
        try (SocketChannel dataChannel = SocketChannel.open(new InetSocketAddress(host, port));
             FileOutputStream fos = new FileOutputStream(partFile);
             FileChannel destChannel = fos.getChannel()) {

            // 1. ส่งคำขอดาวน์โหลด (PARTIAL_REQUEST)
            Protocol.writeInt(dataChannel, Protocol.REQ_DATA_PARTIAL);
            Protocol.writeString(dataChannel, filename);
            Protocol.writeLong(dataChannel, startByte);
            Protocol.writeLong(dataChannel, endByte);
            Protocol.writeString(dataChannel, mode);

            // 2. รับขนาดไฟล์ส่วนย่อย
            long partSize = Protocol.readLong(dataChannel);
            if (partSize == -1) {
                System.err.println("Thread [" + partFile + "] Error: Server could not find file.");
                return;
            }

            System.out.println("Thread [" + partFile + "] receiving " + partSize + " bytes. Mode: " + mode);

            // 3. รับข้อมูลไฟล์
            if (mode.equals("Zero-Copy-MultiThreads")) {
                // --- 🚀 Client-Side Zero-Copy (Mode 3) ---
                System.out.println(" >> Thread [" + partFile + "] using Zero-Copy (transferFrom)...");
                long totalReceived = 0;
                while (totalReceived < partSize) {
                    long received = destChannel.transferFrom(dataChannel, totalReceived, partSize - totalReceived);
                    if (received <= 0) break;
                    totalReceived += received;
                }
            } else {
                // --- Client-Side Copy (Mode 2) ---
                System.out.println(" >> Thread [" + partFile + "] using Copy (read/write)...");
                ByteBuffer buffer = ByteBuffer.allocate(64 * 1024);
                long totalReceived = 0;
                while (totalReceived < partSize && dataChannel.read(buffer) != -1) {
                    buffer.flip();
                    totalReceived += destChannel.write(buffer);
                    buffer.clear();
                }
            }
            
            System.out.println("Thread [" + partFile + "] finished.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}