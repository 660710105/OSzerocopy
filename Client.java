import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Scanner;

public class Client implements Runnable{

    private String host;
    private int port;
    private File targetDir;

    public Client(String host, int port, string pathFile){
        this.host = (host.isEmpty())? 
            "localhost":
             host;

        this.port = port;

        this.targetDir = (pathFile.isEmpty())?
            new File("./mailBox"):
            new File(pathFile);
        
        if (!targetDir.exists())
            targetDir.mkdirs();
    }

    @Override
    public void run() {
        System.out.println("=== Client download at " + targetDir);
        
        try {
            Socket socket = new Socket(host, port);
            ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream());
            System.out.println("=== Connected Server " + host + ": " + port + " ===");

            Object obj = oin.readObject();
            if (!(obj instanceof List)) {
                System.err.println("Error: not list");
                return;
            }

            @SuppressWarnings("unchecked")
            List<String> files = (List<String>) obj;
            System.out.println("=== List files ===");
            for (String f : files)
                System.out.println(files.indexOf(f) + ":  " + f);

            Scanner sc = new Scanner(System.in);
            System.out.println("=== Select File ===");
            System.out.println("Enter index of file:");
            int indexFile = Integer.parseInt(sc.nextLine());
            String filename = files.get(indexFile);

            System.out.println("=== Select Mode === \n 1 = Zero-copy, \n 2 = Buffered");
            System.out.print("Select: ");
            String mode = sc.nextLine();

            oout.writeObject(filename);
            oout.writeObject(mode);
            oout.flush();

            boolean exists = oin.readBoolean();
            if (!exists) {
                System.err.println("File does not exist.");
                return;
            }

            long fileSize = oin.readLong();
            System.out.println("=== Server will send " + fileSize + " bytes. ===");

            File outFile = new File(targetDir, filename);
            long start = System.currentTimeMillis();

            InputStream in = socket.getInputStream();
            FileOutputStream fos = new FileOutputStream(outFile);
            try {
                byte[] buffer = new byte[1024 * 1024];
                long remain = fileSize;
                while (remain > 0) {
                    int read = (buffer.length > remain) ? (int) buffer.length : (int) remain;
                    int r = in.read(buffer, 0, read);
                    fos.write(buffer, 0, r);
                    remain -= r;
                }
                fos.flush();
            } finally {
                long end = System.currentTimeMillis();
                System.out.printf("Downloaded to %s (%d ms)\n", outFile.getAbsolutePath(), (end - start));
                fos.close();
            }
        } catch (SocketException s) {
            System.out.println("Server has shutdown");
            s.printStackTrace();
        } catch (IOException i){
            i.printStackTrace();
        } catch (ClassNotFoundException c) {
            c.printStackTrace();
        }
    }
}