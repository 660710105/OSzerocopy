package zerocopy.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;

import zerocopy.ioutils.Jio;

public class HandlerClient implements Runnable {
    private Socket client;
    private File fileDir;
    private SocketAddress clientAddr; 

    HandlerClient(Socket client, File fileDir) {
        this.client = client;
        this.fileDir = fileDir;
    }

    @Override
    public void run() {
        
        try {
            client.setKeepAlive(true);
            clientAddr = client.getRemoteSocketAddress();
            System.out.println(" >> Client " + clientAddr + " has connected.");

            ObjectOutputStream oout = new ObjectOutputStream(client.getOutputStream());
            ObjectInputStream oin = new ObjectInputStream(client.getInputStream());

            sendFilenameList(oout, fileDir);

            Object name = oin.readObject();
            if (!(name instanceof String)) {
                System.err.println("Error: expected filename string");
            }
            String filename = (String) name;
            System.out.println();
            Object modeNumber = oin.readInt();
            if (!(modeNumber instanceof Integer)) {
                System.err.println("Error: expected mode number");
            }
            int modeIdx = (int) modeNumber;
            String mode = "";
            int nthread = 0;
            switch (modeIdx) {
                case 0:
                    mode = "Copy";
                    break;
                case 1:
                    mode = "Zero-Copy";
                    break;
                case 2:
                    mode = "Copy-MultiThreads";
                    nthread = oin.readInt();
                    break;
                case 3:
                    mode = "Zero-Copy-MultiThreads";
                    nthread = oin.readInt();
                    break;
            }
            ;

            File fileToSend = new File(fileDir, filename);

            long fileSize = fileToSend.length();
            oout.writeLong(fileSize);
            oout.flush();

            System.out.println(clientAddr + " requests file: " + fileToSend
                    + ", mode: " + mode
                    + ", size " + fileSize + " bytes.");

            Jio jio = new Jio();
            FileInputStream fis = new FileInputStream(fileToSend);
            OutputStream outputStream = client.getOutputStream();
            WritableByteChannel wbc = Channels.newChannel(outputStream);
            switch (mode) {
                case "Copy":
                    jio.copyTransfer(fileToSend, fis, outputStream);
                    break;
                case "Zero-Copy":
                    jio.zeroCopyTransfer(fileToSend, fis.getChannel(),
                            wbc);
                    break;
                case "Copy-MultiThreads":
                    jio.multiThread(fileToSend, client.getInetAddress().getHostAddress(),
                                    client.getPort(), nthread,"Copy-Multithreads");
                    break;
                case "Zero-Copy-MultiThreads":
                    jio.multiThread(fileToSend,  client.getInetAddress().getHostAddress(), 
                                    client.getPort(), nthread,"Zero-Copy-Multithreads");
                    break;
                default:
                    jio.copyTransfer(fileToSend, fis, oout);
                    break;
            }
            boolean complete = oin.readBoolean();
            if (complete) {
                System.out.println("complete" + clientAddr);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getCause());
            System.err.println(" >> Client " + clientAddr + " disconnected.");
        }
        // } catch (SocketException s) {
        // System.err.println("Error: " + s.getCause());
        // System.err.println(" >> Client " + clientAddr + " disconnected.");
        // } catch (IOException io){
        // io.printStackTrace();
        // } catch (ClassNotFoundException e) {
        // e.printStackTrace();
    }

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