package zerocopy.server;

import java.io.*;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;

import javax.print.DocFlavor.STRING;

import zerocopy.ioutils.*;

public class Server implements Runnable {
        private File fileDir;
        private int port;

        public Server(String pathFile, int port) {
                this.fileDir = new File(pathFile);
                this.port = port;
        }

        @Override
        public void run() {
                if (!fileDir.exists() || !fileDir.isDirectory()) {
                        throw new IllegalArgumentException("Directory does not exist: " + fileDir.getAbsolutePath());
                }
                // to-do list 1. threads for each client, 2. make some transfer cmd in kind of
                // struct
                try {
                        ServerSocket serverSocket = new ServerSocket(port);
                        System.out.println(
                                        "=== Server listening on port " + port + ", fileDirectory: "
                                                        + fileDir.getAbsolutePath() + " ===");
                        while (true) {
                                Socket client = serverSocket.accept();
                                client.setKeepAlive(true);
                                SocketAddress clientAddr = client.getRemoteSocketAddress();
                                try {
                                        System.out.println(" >> Client " + clientAddr + " has connected.");

                                        ObjectOutputStream oout = new ObjectOutputStream(client.getOutputStream());
                                        ObjectInputStream oin = new ObjectInputStream(client.getInputStream());

                                        sendFilenameList(oout, fileDir);

                                        Object name = oin.readObject();
                                        if (!(name instanceof String)) {
                                                System.err.println("Error: expected filename string");
                                                continue;
                                        }
                                        String filename = (String) name;
                                        System.out.println();
                                        Object modeNumber = oin.readInt();
                                        if (!(modeNumber instanceof Integer)) {
                                                System.err.println("Error: expected mode number");
                                                continue;
                                        }
                                        int modeIdx = (int) modeNumber;
                                        String mode = "";
                                        switch (modeIdx) {
                                                case 0:
                                                        mode = "Copy";
                                                        break;
                                                case 1:
                                                        mode = "Zero-Copy";
                                                        break;
                                                case 2:
                                                        mode = "Buffered";
                                                        break;
                                        };

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
                                                case "Buffered":
                                                        jio.bufferCopyThread(fileToSend, fis.getChannel(),
                                                                        wbc);
                                                default:
                                                        break;
                                        }
                                        boolean complete = oin.readBoolean();
                                        if(complete){
                                                System.out.println("complete" + clientAddr);
                                        }
                                        
                                } catch (SocketException s) {
                                        System.err.println("Error: " + s.getCause());
                                        System.err.println(" >> Client " + clientAddr + " disconnected.");
                                }
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }

        private void sendFilenameList(ObjectOutputStream oout, File file) {
                try {
                        // ObjectOutputStream oout = new ObjectOutputStream(client.getOutputStream());

                        // System.out.println(" >> Sent list of files to client " +
                        // client.getRemoteSocketAddress());

                        File[] files = file.listFiles();
                        List<String> listFileName = new ArrayList<>();
                        for (File f : files) {
                                if (f instanceof File)
                                        listFileName.add(f.getName());
                        }
                        oout.writeObject(listFileName);
                        oout.flush();
                } catch (IOException e) {
                        // System.err.println(
                        // "Error sending file list to client " + client.getRemoteSocketAddress() + ": "
                        // + e.getMessage());
                }
        }
}
