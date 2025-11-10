package zerocopy.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import zerocopy.fileutils.Filefly;
import zerocopy.ioutils.Jio;
import zerocopy.ioutils.notation.Size;
import zerocopy.ioutils.notation.SizeConverter;
import zerocopy.ioutils.notation.SizeNotation;

public class HandlerClient implements Runnable {
    private Socket client;
    private Filefly filefly;
    private SocketAddress clientAddr; 

    HandlerClient(Socket client, Filefly filefly) {
        this.client = client;
        this.filefly = filefly;
    }

    @Override
    public void run() {
        
        try {
            client.setKeepAlive(true);
            clientAddr = client.getRemoteSocketAddress();
            System.out.println(" >> Client " + clientAddr + " has connected.");

            ObjectOutputStream oout = new ObjectOutputStream(client.getOutputStream());
            ObjectInputStream oin = new ObjectInputStream(client.getInputStream());

            oout.writeObject(filefly);

            int fileIdx = oin.readInt();
            int modeIdx = oin.readInt();
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
            };

            File fileToSend = filefly.getFile(fileIdx);

            long fileSize = fileToSend.length();
            oout.writeLong(fileSize);
            oout.flush();

            System.out.println(clientAddr + " requests file: " + fileToSend
                               + ", mode: " + mode
                               + ", size " + SizeConverter
                               .toHighestSize(new Size(SizeNotation.B,(fileSize))).toString());

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
    }
}
