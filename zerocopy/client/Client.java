package zerocopy.client;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Scanner;

public class Client implements Runnable {
        private String host;
        private int port;
        private File targetDir;

        public Client(String host, int port, String pathFile){
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

                        oin = new ObjectInputStream(socket.getInputStream());
                        oout = new ObjectOutputStream(socket.getOutputStream());

                        Object obj = oin.read();

                        @SuppressWarnings("unchecked") List<String> listFilenames = (List<String>)obj;
                        
                        System.out.println("=== List files ===");
                        
                        for (int i=0 ; i < listFilenames.size(); i++) {
                                System.out.println( i + ":  " + listFilenames.get(i));
                        }
                        
                        System.out.println("=== Select File ===");
                        System.out.print("Enter index of file: ");
                        int indexFile = Integer.parseInt(sc.nextLine());
                        String filename = listFilenames.get(indexFile);

                        System.out.println("=== Select Mode === \n 0 = Copy \n 1 = Zero-copy \n 2 = Buffered");
                        System.out.print("Select: ");
                        String mode = sc.nextLine();

                        oout.writeObject(filename);
                        oout.writeObject(mode);
                        oout.flush();

                        boolean exists = oin.readBoolean();
                        System.out.println(exists);

                        // long fileSize = oin.readLong();
                        // System.out.println("=== Server will send " + fileSize + " bytes. ===");

                        File outFile = new File(targetDir, filename);
                        FileOutputStream fos = new FileOutputStream(outFile);
                        long start = System.currentTimeMillis();

                        byte[] fileContent = (byte[]) oin.readObject();
                        fos.write(fileContent);
                        
                        fos.close();
                        
                        System.out.println("self destruction");

                        // InputStream in = socket.getInputStream();
            
                        // try {
                        //     byte[] buffer = new byte[1024 * 1024];
                        //     long remain = fileSize;
                        //     while (remain > 0) {
                        //         int read = (buffer.length > remain) ? (int) buffer.length : (int) remain;
                        //         int r = in.read(buffer, 0, read);
                        //         fos.write(buffer, 0, r);
                        //         remain -= r;
                        //     }
                        //     fos.flush();
                        // } finally {
                        //     long end = System.currentTimeMillis();
                        //     System.out.printf("Downloaded to %s (%d ms)\n", outFile.getAbsolutePath(), (end - start));
                        //     fos.close();
                        // }

                        // closing this will throw exception, so it needed to be is this block.
                        oin.close();
                        oout.close();
                        socket.close();
                } catch (SocketException s) {
                        System.out.println("Disconnected server");
                        s.printStackTrace();
                } catch (IOException i){
                        i.printStackTrace();
                } catch (ClassNotFoundException c) {
                        c.printStackTrace();
                } finally {
                        sc.close();
                }
        }
}
