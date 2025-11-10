package zerocopy.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import zerocopy.common.CopyMode;
import zerocopy.fileutils.Filefly;
import zerocopy.ioutils.Jio;
import zerocopy.ioutils.notation.Size;
import zerocopy.ioutils.notation.SizeConverter;
import zerocopy.ioutils.notation.SizeNotation;

public class HandlerClient implements Runnable {
    private Socket client;
    private Filefly filefly;
    private SocketAddress clientAddr;
    private Jio jio;

    HandlerClient(Socket client, Filefly filefly) {
        this.client = client;
        this.filefly = filefly;
        this.jio = new Jio();
    }

    @Override
    public void run() {
        try {
            client.setKeepAlive(true);
            
            clientAddr = client.getRemoteSocketAddress();
            
            System.out.println(" >> Client " + clientAddr + " has connected.");

            ObjectOutputStream oout = new ObjectOutputStream(client.getOutputStream());
            ObjectInputStream oin = new ObjectInputStream(client.getInputStream());
            OutputStream clientOutputStream = client.getOutputStream();
            InputStream clientInputStream = client.getInputStream();

            oout.writeObject(filefly);

            int fileIdx = oin.readInt();
            CopyMode mode = (CopyMode) oin.readObject();

            int nthOfThread = -1;
            if (mode == CopyMode.COPY_MULTITHREAD || mode == CopyMode.ZEROCOPY_MULTITHREAD) {
                nthOfThread = oin.readInt();
            }
            
            File fileToSend = filefly.getFile(fileIdx);
            
            long fileSize = filefly.getFile(fileIdx).length();
            Size _rawFileSize = new Size(SizeNotation.B, fileSize);
            Size _displayFileSize = SizeConverter.toHighestSize(_rawFileSize);

            System.out.println(clientAddr + " requests file: " + fileToSend
                               + ", mode: " + mode
                               + ", size " + _displayFileSize.toString());        

            FileInputStream fis = new FileInputStream(fileToSend);
            WritableByteChannel wbc = Channels.newChannel(clientOutputStream);
            
            switch (mode) {
            case COPY:
                jio.copyTransfer(fileToSend, fis, clientOutputStream);
                break;
            case ZEROCOPY:
                jio.zeroCopyTransfer(fileToSend,
                                     fis.getChannel(),
                                     wbc);
                
                break;
            case COPY_MULTITHREAD:
                jio.multiThread(fileToSend,
                                client.getInetAddress().getHostAddress(),
                                client.getPort(),
                                nthOfThread,
                                mode);
                
                break;
            case ZEROCOPY_MULTITHREAD:
                jio.multiThread(fileToSend,
                                client.getInetAddress().getHostAddress(), 
                                client.getPort(),
                                nthOfThread,
                                mode);
                
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
