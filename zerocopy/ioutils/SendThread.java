package zerocopy.ioutils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

public class SendThread implements Runnable {
    private String host, filename, mode, partFile;
    private int port;
    private long startByte, endByte;
    private static int BUFFER_SIZE = 64 * 1024;

    public SendThread(File file, String host, int port, long startByte, long endByte, String partFile, String mode) {
        this.filename = file.getName();
        this.startByte = startByte;
        this.endByte = endByte;
        this.partFile = partFile;
        this.host = host;
        this.port = port;
        this.mode = mode;
    }

    @Override
    public void run() {
        Socket socket = null;
        FileOutputStream fos = null;
        ObjectOutputStream oout = null; // <--- ย้ายไปประกาศข้างนอก
        ObjectInputStream oin = null; // <--- ย้ายไปประกาศข้างนอก
        InputStream in = null; // <--- ย้ายไปประกาศข้างนอก

        try {

            // 1. เชื่อมต่อด้วย Socket ธรรมดา
            socket = new Socket(host, port);

            // 2. สร้าง Stream
            oout = new ObjectOutputStream(socket.getOutputStream());
            oin = new ObjectInputStream(socket.getInputStream());
            in = socket.getInputStream();

            // 3. เปิดไฟล์ .part ที่จะเขียน
            fos = new FileOutputStream(partFile);

            // --- ส่วนที่ 1: ส่งคำขอแบบใหม่ (PARTIAL_REQUEST) ---
            oout.writeObject("PARTIAL_REQUEST"); // 1. บอก Server ว่าเป็นเธรดลูก
            oout.writeObject(filename);          // 2. ชื่อไฟล์
            oout.writeLong(startByte);           // 3. ไบต์เริ่มต้น
            oout.writeLong(endByte);             // 4. ไบต์สิ้นสุด
            oout.writeObject(mode);              // 5. โหมด (เพื่อให้ Server รู้ว่าต้องใช้ partialCopy หรือ partialZeroCopy)
            oout.flush();
            // ------------------------------------------------

            System.out.println("Thread [" + partFile + "] requesting: " + filename + " bytes " + startByte + " to " + endByte);

            // --- ส่วนที่ 2: รับขนาดไฟล์ (ส่วนย่อย) ที่ Server จะส่งมา ---
            long partSize = oin.readLong();
            if (partSize == -1) {
                System.err.println("Thread [" + partFile + "] Error: Server could not find file.");
                return;
            }
            // ------------------------------------------------
            System.out.println("Thread [" + partFile + "] writing to " + partFile + " (" + partSize + " bytes)");

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long remain = partSize;
            while (remain > 0 && (bytesRead = in.read(buffer, 0, (int) Math.min(remain, BUFFER_SIZE))) > 0) {
                fos.write(buffer, 0, bytesRead); // <-- เขียนลง FileOutputStream
                remain -= bytesRead;
            }
            
            System.out.println("Thread [" + startByte + "] finished.");


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // ปิดทรัพยากรทั้งหมด
            try { if (fos != null) fos.close(); } catch (IOException e) { e.printStackTrace(); } // <--- นำกลับมา
            try { if (in != null) in.close(); } catch (IOException e) { e.printStackTrace(); }
            try { if (oin != null) oin.close(); } catch (IOException e) { e.printStackTrace(); }
            try { if (oout != null) oout.close(); } catch (IOException e) { e.printStackTrace(); }
            try { if (socket != null) socket.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }
}