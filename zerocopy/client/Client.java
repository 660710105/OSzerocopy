package zerocopy.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// เพิ่ม import
import zerocopy.ioutils.Jio;
import zerocopy.ioutils.Protocol;

public class Client implements Runnable {
    private String host;
    private int port;
    private File targetDir;
    private Jio jio;

    public Client(String host, int port, String pathFile) {
        this.host = host;
        this.port = port;
        this.targetDir = new File(pathFile);
        this.jio = new Jio();

        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
    }

    @Override
    public void run() {
        Scanner sc = new Scanner(System.in);

        try (SocketChannel controlChannel = SocketChannel.open(new InetSocketAddress(host, port))){
            System.out.println(" >> Client download at " + targetDir);
            System.out.println(" >> Connected Server (Control Plane) " + host + ": " + port);

            // 1. รับรายชื่อไฟล์
            List<String> listFileName = receiveFileList(controlChannel);

            System.out.println("File list: ");
            for (String f : listFileName) {
                System.out.println("\t" + f);
            }

            System.out.println();
            System.out.println("Mode: ");
            String[] modes = { "Copy", "Zero-Copy", "Copy-MultiThreads", "Zero-Copy-MultiThreads" };
            for (int i = 0; i < modes.length; i++) {
                System.out.println("\t" + i + ". " + modes[i]);
            }
            System.out.println();

            System.out.print("Select file name: ");
            String filename = sc.nextLine();
            System.out.print("Select mode: ");
            int modeIdx = sc.nextInt();
            String mode = modes[modeIdx];

            
            // --- ส่วนที่ 2: ถามจำนวนเธรด ถ้าเลือกโหมด 2 หรือ 3 ---
            int nthread = 1;
            if (modeIdx == 2 || modeIdx == 3) {
                System.out.print("Select number of threads: ");
                nthread = sc.nextInt();
            }
            // --------------------------------------------------

            long fileSize = sendMainRequest(controlChannel, filename, modeIdx, nthread);
            if (fileSize == -1) {
                System.err.println("Server could not find file: " + filename);
                return;
            }
            System.out.println(" >> Receiving file: " + filename + " (" + fileSize + " bytes)");

            File file = new File(targetDir, filename);
            long start = System.currentTimeMillis();

            // 3. จัดการการดาวน์โหลด
            switch (mode) {
                case "Copy": // Mode 0
                case "Zero-Copy": // Mode 1
                    
                    // --- 🚀 End-to-End Zero-Copy (Mode 1) ---
                    // เปิด "Data Plane" (Socket ใหม่) เพื่อรับไฟล์
                    downloadSingleFile(file, filename, fileSize, (modeIdx == 1));
                    break;
                    
                case "Copy-MultiThreads": // Mode 2
                case "Zero-Copy-MultiThreads": // Mode 3
                    // โค้ดนี้จะใช้ "ไฟล์ย่อย + รวมไฟล์" ที่แก้ปัญหาคอขวดแล้ว
                    jio.multiThread(file, fileSize, host, port, nthread, mode, targetDir);
                    break;
            }


            long end = System.currentTimeMillis();
            System.out.printf("Downloaded " + filename
                    + ", mode: " + mode
                    + " (%.2f s)\\n", ((end - start) / 1000.0));
            
            // 4. ส่งสัญญาณ "เสร็จสิ้น" บน Control Plane
            Protocol.writeInt(controlChannel, Protocol.REQ_COMPLETE);
            
        } catch (SocketException s) {
            System.out.println("Disconnected from server");
            // s.printStackTrace();
        } catch (IOException i) {
            i.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            sc.close();
        }
    }

    // (Helper 1) รับรายชื่อไฟล์
    private List<String> receiveFileList(SocketChannel channel) throws IOException {
        Protocol.writeInt(channel, Protocol.REQ_FILE_LIST); // 1. ขอรายชื่อไฟล์
        int fileCount = Protocol.readInt(channel); // 2. รับจำนวนไฟล์
        
        List<String> list = new ArrayList<>();
        for (int i = 0; i < fileCount; i++) {
            list.add(Protocol.readString(channel)); // 3. วนลูปรับชื่อไฟล์
        }
        return list;
    }

    // (Helper 2) ส่งคำขอหลักและรับขนาดไฟล์
    private long sendMainRequest(SocketChannel channel, String filename, int modeIdx, int nthread) throws IOException {
        Protocol.writeInt(channel, Protocol.REQ_MAIN); // 1. บอกว่านี่คือคำขอหลัก
        Protocol.writeString(channel, filename); // 2. ส่งชื่อไฟล์
        Protocol.writeInt(channel, modeIdx); // 3. ส่งโหมด
        Protocol.writeInt(channel, nthread); // 4. ส่งจำนวนเธรด
        
        return Protocol.readLong(channel); // 5. รับขนาดไฟล์กลับมา
    }

    // (Helper 3) โหลดไฟล์ (โหมด 0 และ 1)
    private void downloadSingleFile(File file, String filename, long fileSize, boolean isZeroCopy) throws IOException {
        // เปิด "Data Plane" (Socket ใหม่)
        try (SocketChannel dataChannel = SocketChannel.open(new InetSocketAddress(host, port));
             FileOutputStream fos = new FileOutputStream(file);
             FileChannel destChannel = fos.getChannel()) {
            
            System.out.println(" >> Data Plane connected for single file download...");

            // 1. ส่งคำขอดาวน์โหลด (DATA_REQUEST)
            Protocol.writeInt(dataChannel, Protocol.REQ_DATA_SINGLE);
            Protocol.writeString(dataChannel, filename);
            Protocol.writeInt(dataChannel, isZeroCopy ? 1 : 0); // 1=ZeroCopy, 0=Copy
            
            // 2. รับข้อมูลไฟล์
            if (isZeroCopy) {
                // --- 🚀 Client-Side Zero-Copy (Mode 1) ---
                System.out.println(" >> Client receiving via Zero-Copy (transferFrom)...");
                long totalReceived = 0;
                while (totalReceived < fileSize) {
                    long received = destChannel.transferFrom(dataChannel, totalReceived, fileSize - totalReceived);
                    if (received <= 0) break;
                    totalReceived += received;
                }
            } else {
                // --- Client-Side Copy (Mode 0) ---
                System.out.println(" >> Client receiving via Copy (read/write)...");
                ByteBuffer buffer = ByteBuffer.allocate(8 * 1024);
                long totalReceived = 0;
                while (totalReceived < fileSize && dataChannel.read(buffer) != -1) {
                    buffer.flip();
                    totalReceived += destChannel.write(buffer);
                    buffer.clear();
                }
            }
        }
    }
}