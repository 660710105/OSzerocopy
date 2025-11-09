package zerocopy.client;

import java.io.*;
import java.net.*;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Scanner;

public class Client implements Runnable {
        private String host;
        private int port;
        private File targetDir;
        private static int BUFFER_SIZE = 8 * 1024;

        public Client(String host, int port, String pathFile) {
                this.host = host;
                this.port = port;
                this.targetDir = new File(pathFile);

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

                        Object obj = oin.readObject();

                        @SuppressWarnings("unchecked")
                        List<String> listFilenames = (List<String>) obj;

                        System.out.println("=== List files ===");

                        for (int i = 0; i < listFilenames.size(); i++) {
                                System.out.println(i + ":  " + listFilenames.get(i));
                        }

                        System.out.println("=== Select File ===");
                        System.out.print("Enter index of file: ");
                        int indexFile = Integer.parseInt(sc.nextLine());
                        if (indexFile >= listFilenames.size()) {
                                System.err.println("Error: File index out of bound");
                        }
                        String filename = listFilenames.get(indexFile);

                        System.out.println("=== Select Mode === \n "
                                        + " 0 = Copy \n"
                                        + " 1 = Zero-Copy \n"
                                        + " 2 = Copy-MultiThreads \n"
                                        + " 3 = Zero-Copy-MultiThreads");
                        System.out.print("Select: ");
                        int modeIdx = sc.nextInt();
                        if (modeIdx > 3) {
                                System.err.println("Error: Mode not found");
                        }
                        String mode = "";
                        switch (modeIdx) {
                                case 0:
                                        mode = "Copy";
                                        break;
                                case 1:
                                        mode = "Zero-Copy";
                                        break;
                                case 2:
                                        mode = "Copy-MultiThreads";
                                        break;
                                case 3:
                                        mode = "Zero-Copy-MultiThreads";
                                        break;
                        }

                        oout.writeObject(filename);
                        oout.writeInt(modeIdx);
                        oout.flush();

                        long fileSize = oin.readLong();
                        System.out.printf(" >> Server will send .%2f KB.", fileSize / 1000.0);

                        File outFile = new File(targetDir, filename);
                        FileOutputStream fos = new FileOutputStream(outFile);
                        long start = System.currentTimeMillis();
                        InputStream in = socket.getInputStream();

                        byte[] buffer = new byte[64 * 1024];
                        long remain = fileSize;
                        switch (mode) {
                                case "Copy":
                                case "Zero-Copy":
                                        while (remain > 0) {
                                                int toRead = (int) Math.min(buffer.length, remain);
                                                int r = in.read(buffer, 0, toRead);
                                                if (r < 0)
                                                        throw new EOFException("Unexpected EOF");
                                                fos.write(buffer, 0, r);
                                                remain -= r;
                                                System.out.printf("Download %.2f KB.\n",
                                                                (fileSize - remain) / 1000.0);
                                        }
                                        fos.flush();
                                        break;
                                case "Copy-MultiThreads":
                                case "Zero-Copy-MultiThreads":
                                        
                                        break;
                        }

                        long end = System.currentTimeMillis();
                        System.out.printf("Sent " + filename
                                        + ", mode" + mode
                                        + " (%.2f s)\n", ((end - start) / 1000.0));
                        oout.writeBoolean(true); // send complete
                        oout.flush();
                        Thread.sleep(2000);
                        oin.close();
                        oout.close();
                        in.close();
                        fos.close();
                        socket.close();
                } catch (SocketException s) {
                        System.out.println("Disconnected server");
                        s.printStackTrace();
                } catch (IOException i) {
                        i.printStackTrace();
                } catch (ClassNotFoundException c) {
                        c.printStackTrace();
                } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                } finally {
                        sc.close();
                }
        }
}
