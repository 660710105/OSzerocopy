package zerocopy.ioutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel; // <--- เพิ่ม
import java.nio.channels.SocketChannel; // <--- เพิ่ม
import java.nio.channels.WritableByteChannel;

public class Jio {
    private static int BUFFER_SIZE = 64 * 1024;

    // (Copy) สำหรับ Mode 0
    public void copyTransfer(File file, InputStream fileIn, OutputStream socketOut) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = fileIn.read(buffer)) != -1) {
            socketOut.write(buffer, 0, bytesRead);
        }
        socketOut.flush();
    }

    // (Zero-Copy) สำหรับ Mode 1
    public void zeroCopyTransfer(File file, FileChannel fileChannel, WritableByteChannel wbc) throws IOException {
        try {
            long fileSize = file.length();
            long position = 0;
            while (position < fileSize) {
                long transferred = fileChannel.transferTo(position, fileSize - position, wbc);
                if (transferred <= 0) break;
                position += transferred;
            }
        } catch (SocketException s) {
           System.err.println("Socket exception during zero-copy: " + s.getMessage());
        }
    }

    // (Copy) สำหรับ Mode 2
    public void partialCopyTransfer(InputStream fileIn, SocketChannel socketChannel, long partSize) throws IOException {
        // เราใช้ SocketChannel.transferFrom(fileIn) ไม่ได้ (มันไม่รองรับ InputStream)
        // ดังนั้นเรายังคงต้องใช้ Buffer
        OutputStream os = java.nio.channels.Channels.newOutputStream(socketChannel);
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        long remaining = partSize;
        
        while (remaining > 0 && (bytesRead = fileIn.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
            os.write(buffer, 0, bytesRead);
            remaining -= bytesRead;
        }
        os.flush();
    }

    // (Zero-Copy) สำหรับ Mode 3
    public void partialZeroCopyTransfer(FileChannel fileChannel, WritableByteChannel wbc, long startPosition, long partSize) throws IOException {
        try {
            long position = startPosition;
            long count = partSize;
            while (count > 0) {
                long transferred = fileChannel.transferTo(position, count, wbc);
                if (transferred <= 0) break;
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
    // (Multi-Thread) จัดการฝั่ง Client
    public void multiThread(File file, long fileSize, String host, int port, int nthread, String mode, File targetDir) throws IOException, InterruptedException {
        long partSize = fileSize / nthread;
        Thread[] threads = new Thread[nthread];
        File[] partFiles = new File[nthread];

        System.out.println("Starting " + nthread + " threads, downloading to temporary part-files...");

        for (int i = 0; i < nthread; i++) {
            long startByte = i * partSize;
            long endByte = (i == nthread - 1) ? fileSize - 1 : (startByte + partSize - 1);
            
            String tempPartName = file.getName() + ".part." + i;
            partFiles[i] = new File(targetDir, tempPartName);
            
            // เรียก SendThread (เวอร์ชัน NIO ใหม่)
            SendThread worker = new SendThread(file.getName(), host, port, startByte, endByte, partFiles[i].getAbsolutePath(), mode);
            threads[i] = new Thread(worker);
            threads[i].start();
        }

        // รอทุกเธรดดาวน์โหลดให้เสร็จ
        for (int i = 0; i < nthread; i++) {
            threads[i].join();
        }

        System.out.println("All threads finished downloading. Starting file reassembly...");

        // (โค้ดรวมไฟล์ - Zero-Copy Merge)
        try (FileOutputStream fos = new FileOutputStream(file);
             FileChannel destChannel = fos.getChannel()) {
            
            for (int i = 0; i < nthread; i++) {
                if (!partFiles[i].exists()) continue;
                try (FileInputStream fis = new FileInputStream(partFiles[i]);
                     FileChannel srcChannel = fis.getChannel()) {
                    System.out.println("Appending " + partFiles[i].getName());
                    srcChannel.transferTo(0, srcChannel.size(), destChannel);
                }
                partFiles[i].delete();
            }
        }
        System.out.println("File reassembly complete.");
    }
}