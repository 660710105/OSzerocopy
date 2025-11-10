package zerocopy.ioutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

public class Jio {
    private static int BUFFER_SIZE = 64 * 1024;

    public void copyTransfer(File file, InputStream fileIn, OutputStream socketOut) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = fileIn.read(buffer)) != -1) {
            socketOut.write(buffer, 0, bytesRead);
        }
        socketOut.flush();
    }

    public void zeroCopyTransfer(File file, FileChannel fileChannel, WritableByteChannel wbc) throws IOException {
        try {
            long fileSize = file.length();
            long position = 0;
            while (position < fileSize) {
                long transferred = fileChannel.transferTo(position, fileSize - position, wbc);

                if (transferred <= 0) {
                    System.err.println("Transfer stalled or socket closed.");
                    break;
                }

                position += transferred;
            }
        } catch (SocketException s) {
           s.printStackTrace();
        }
    }
    // --- เมธอดใหม่ที่ 1: ส่งไฟล์ "บางส่วน" (Copy) ---
    public void partialCopyTransfer(InputStream fileIn, OutputStream socketOut, long partSize) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        long remaining = partSize;
        
        while (remaining > 0 && (bytesRead = fileIn.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
            socketOut.write(buffer, 0, bytesRead);
            remaining -= bytesRead;
        }
        socketOut.flush();
    }
    // --- เมธอดใหม่ที่ 2: ส่งไฟล์ "บางส่วน" (Zero-Copy) ---
    public void partialZeroCopyTransfer(FileChannel fileChannel, WritableByteChannel wbc, long startPosition, long partSize) throws IOException {
        try {
            long position = startPosition;
            long count = partSize;
            while (count > 0) {
                long transferred = fileChannel.transferTo(position, count, wbc);
                if (transferred <= 0 && count > 0) {
                     System.err.println("Partial transfer stalled or socket closed.");
                    break;
                }
                position += transferred;
                count -= transferred;
            }
        } catch (SocketException s) {
           System.err.println("Socket exception during partial zero-copy: " + s.getMessage());
        }
    }
    // ----------------------------------------------------

    /**
     * (แก้ไข) จัดการการดาวน์โหลดแบบหลายเธรด (ฝั่ง Client) และ "รวมไฟล์"
     */
    public void multiThread(File file, long fileSize,String host, int port, int nthread, String mode, File targetDir) throws IOException, InterruptedException {
        long partSize = fileSize / nthread;
        
        Thread[] threads = new Thread[nthread];
        File[] partFiles = new File[nthread];

        System.out.println("Starting " + nthread + " threads, downloading to temporary part-files...");

        for (int i = 0; i < nthread; i++) {
            long startByte = i * partSize;
            long endByte;

            // แก้ไข: เธรดสุดท้ายต้องดาวน์โหลดจนจบไฟล์
            if (i == nthread - 1) {
                endByte = fileSize - 1;
            } else {
                endByte = startByte + partSize - 1;
            }

            String tempPartName = file.getName() + ".part." + i;
            partFiles[i] = new File(targetDir, tempPartName);
            
            // ส่ง "mode" (String) ไปให้ SendThread ด้วย
            SendThread worker = new SendThread(file, host, port, startByte, endByte,partFiles[i].getAbsolutePath() , mode);
            threads[i] = new Thread(worker);
            threads[i].start();
        }

        // รอทุกเธรดดาวน์โหลดให้เสร็จ
        for (int i = 0; i < nthread; i++) {
            threads[i].join();
        }

        System.out.println("All threads finished writing.");

        // --- ส่วนที่ 3: รวมไฟล์ (Reassembly) ---
        // ใช้ FileChannel (Zero-Copy) ในการรวมไฟล์ จะเร็วมาก
        try (FileOutputStream fos = new FileOutputStream(file); // 'file' คือไฟล์ปลายทาง
             FileChannel destChannel = fos.getChannel()) {
            
            for (int i = 0; i < nthread; i++) {
                if (!partFiles[i].exists()) {
                    System.err.println("Error: Missing part file: " + partFiles[i].getName());
                    continue;
                }
                try (FileInputStream fis = new FileInputStream(partFiles[i]);
                     FileChannel srcChannel = fis.getChannel()) {
                    
                    System.out.println("Appending " + partFiles[i].getName());
                    srcChannel.transferTo(0, srcChannel.size(), destChannel);
                }
                // ลบไฟล์ part
                partFiles[i].delete();
            }
        }
        System.out.println("File reassembly complete.");
        // ------------------------------------------
        
    }
}