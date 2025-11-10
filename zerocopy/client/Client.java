package zerocopy.client;

import java.io.*;
import java.net.*;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Scanner;

// เพิ่ม import
import zerocopy.ioutils.Jio;

public class Client implements Runnable {
    private String host;
    private int port;
    private File targetDir;
    private static int BUFFER_SIZE = 8 * 1024;
    
    // เพิ่ม Jio utility
    private Jio jio;

    public Client(String host, int port, String pathFile) {
        this.host = host;
        this.port = port;
        this.targetDir = new File(pathFile);
        this.jio = new Jio(); // สร้างอินสแตนซ์

        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
    }

    @Override
    public void run() {
        Scanner sc = new Scanner(System.in);
        Socket socket;
        ObjectInputStream oin;
        ObjectOutputStream oout;

        try {
            System.out.println(" >> Client download at " + targetDir);

            socket = new Socket(host, port);
            socket.setKeepAlive(true);

            System.out.println(" >> Connected Server " + host + ": " + port);

            oout = new ObjectOutputStream(socket.getOutputStream());
            oin = new ObjectInputStream(socket.getInputStream());

            // --- ส่วนที่ 1: ส่ง Request Type เพื่อบอก Server ว่านี่คือ "การเชื่อมต่อหลัก" ---
            oout.writeObject("MAIN_REQUEST");
            oout.flush();
            // ---------------------------------------------------------------------

            Object obj = oin.readObject();

            @SuppressWarnings("unchecked")
            List<String> listFileName = (List<String>) obj;

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

            oout.writeObject(filename);
            oout.writeInt(modeIdx);
            
            // --- ส่วนที่ 3: ส่งจำนวนเธรดไปให้ Server (หลัก) ---
            if (modeIdx == 2 || modeIdx == 3) {
                oout.writeInt(nthread);
            }
            // ----------------------------------------------
            
            oout.flush();

            long fileSize = oin.readLong();
            System.out.println(" >> Receiving file: " + filename + " (" + fileSize + " bytes)");
            
            // สร้าง File object สำหรับไฟล์ปลายทาง
            File file = new File(targetDir, filename);
            FileOutputStream fos = null; // ย้ายไปประกาศข้างนอก
            
            if(nthread == 1 && modeIdx == 2) {
                mode = "Copy";
                System.out.println(" >> Optimizing: 1 thread selected, falling back to 'Copy' (Mode 0)");
            }
            if(nthread == 1 && modeIdx == 3){
                mode = "Zero-Copy";
                System.out.println(" >> Optimizing: 1 thread selected, falling back to 'Zero-Copy' (Mode 1)");
            }


            long start = System.currentTimeMillis();

            switch (mode) {
                case "Copy":
                case "Zero-Copy":
                    // โหมด 0 และ 1 ทำงานเหมือนเดิม (ดาวน์โหลดในเธรดหลัก)
                    fos = new FileOutputStream(file);
                    InputStream in = socket.getInputStream(); // ใช้อันนี้สำหรับ Copy
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int r;
                    long remain = fileSize;
                    while (remain > 0 && (r = in.read(buffer, 0, (int) Math.min(remain, BUFFER_SIZE))) != -1) {
                        fos.write(buffer, 0, r);
                        remain -= r;
                    }
                    fos.close(); // ปิด
                    break;
                    
                // --- ส่วนที่ 4: เรียกใช้ jio.multiThread สำหรับโหมด 2 และ 3 ---
                case "Copy-MultiThreads":
                case "Zero-Copy-MultiThreads":
                    // โหมด 2 และ 3 จะใช้ jio.multiThread จัดการ
                    // เธรดหลักนี้จะรอจนกว่าการดาวน์โหลดแบบหลายเธรด (และการรวมไฟล์) จะเสร็จสิ้น
                    try {
                        jio.multiThread(file, fileSize, host, port, nthread, mode, targetDir);
                    } catch (InterruptedException e) {
                        System.err.println("Multi-thread download interrupted.");
                        e.printStackTrace();
                    }
                    break;
                // --------------------------------------------------------
            }

            long end = System.currentTimeMillis();
            System.out.printf("Downloaded " + filename
                    + ", mode: " + mode
                    + " (%.2f s)\\n", ((end - start) / 1000.0));
            
            oout.writeBoolean(true); // send complete
            oout.flush();
            Thread.sleep(2000);
            
            oin.close();
            oout.close();
            
            // ป้องกัน NullPointerException ถ้า fos ไม่ได้ถูกสร้าง (ในโหมด 2, 3)
            if (fos != null) {
                fos.close();
            }
            socket.close();
            
        } catch (SocketException s) {
            System.out.println("Disconnected from server");
            // s.printStackTrace();
        } catch (IOException i) {
            i.printStackTrace();
        } catch (ClassNotFoundException c) {
            c.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            sc.close();
        }
    }
}