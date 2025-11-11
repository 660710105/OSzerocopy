package zerocopy.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

import zerocopy.common.CopyMode;
import zerocopy.fileutils.FileflyServer;
import zerocopy.ioutils.Jio;
import zerocopy.ioutils.notation.Size;
import zerocopy.ioutils.notation.SizeConverter;
import zerocopy.ioutils.notation.SizeNotation;

public class HandlerClient implements Runnable {
    private Socket client;
    private FileflyServer fileflyServer;
    private SocketAddress clientAddr;
    private Jio jio;

    HandlerClient(Socket client, FileflyServer fileflyServer) {
        this.client = client;
        this.fileflyServer = fileflyServer;
        this.jio = new Jio();
    }

    @Override
    public void run() {
        try {
            client.setKeepAlive(true);
            
            OutputStream clientOutputStream = client.getOutputStream();
            InputStream clientInputStream = client.getInputStream();
            ObjectOutputStream oout = new ObjectOutputStream(clientOutputStream);
            ObjectInputStream oin = new ObjectInputStream(clientInputStream);

            
            clientAddr = client.getRemoteSocketAddress();

            String protocol = (String) oin.readObject();
            switch (protocol) {
                case "CLIENT_REQUEST":
                    System.out.println(" >> Client " + clientAddr + " has connected.");
                    handleClientRequest(clientOutputStream, clientInputStream, oout, oin);
                    break;
                case "FILE_REQUEST":
                    System.out.println(" >> FileThread " + clientAddr + " has connected.");
                    handleFileRequest(clientOutputStream, clientInputStream, oout, oin);
                    break;
                default:
                    System.err.println(" >> Unknown Request Protocol.");
                    break;
            };
        } catch (Exception e) {
            // e.printStackTrace();
            System.err.println(" >> Client " + clientAddr + " disconnected abruptly.");
        } finally {
            try {
                if (client != null && !client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleClientRequest(OutputStream clientOutputStream, InputStream clInputStream, 
                                    ObjectOutputStream oout, ObjectInputStream oin){
        try{
            oout.writeObject(fileflyServer.convertToFileLists());

            int fileIdx = oin.readInt();
            CopyMode mode = (CopyMode) oin.readObject();

            int nthOfThread = 1;
            if (mode == CopyMode.COPY_MULTITHREAD || mode == CopyMode.ZEROCOPY_MULTITHREAD) {
                nthOfThread = oin.readInt();
            }
            
            File fileToSend = fileflyServer.getFile(fileIdx);
            
            long fileSize = fileflyServer.getFile(fileIdx).length();
            Size _rawFileSize = new Size(SizeNotation.B, fileSize);
            Size _displayFileSize = SizeConverter.toHighestSize(_rawFileSize);

            System.out.println(clientAddr + " requests file: " + fileToSend
                               + ", mode: " + mode
                               + ", size " + _displayFileSize.toString());        

            FileInputStream fis = new FileInputStream(fileToSend);
            WritableByteChannel wbc = Channels.newChannel(clientOutputStream);
            
            switch (mode) {
            case COPY:
                jio.copyTransfer(fis, clientOutputStream);
                break;
            case ZEROCOPY:
                jio.zeroCopyTransfer(fis.getChannel(),
                                     wbc, fileSize);
                
                break;
            case COPY_MULTITHREAD:
            case ZEROCOPY_MULTITHREAD:
                System.out.println("Sending File with "+ nthOfThread + " threads at " + clientAddr);
                break;
            default:
                jio.copyTransfer(fis, oout);
                break;
            }
            
            boolean complete = oin.readBoolean();
            if (complete) {
                System.out.println(" >> Send file to " + clientAddr+ " completed.");
            }
        } catch (Exception e) {
            System.err.println("Error in Client Request: " + e.getCause());
        }
    }

    private void handleFileRequest(OutputStream clientOutputStream, InputStream clientInputStream, 
                                    ObjectOutputStream oout, ObjectInputStream oin){
        try{
            String filename = (String) oin.readObject();
            long startByte = oin.readLong();
            long endByte = oin.readLong();
            CopyMode mode = (CopyMode) oin.readObject();

            File fileToSend = new File(fileflyServer.getOwnFile(), filename);
            if (!fileToSend.exists()) {
                System.err.println("Thread " + clientAddr + " requested non-existent file: " + filename);
                oout.writeLong(-1);
                oout.flush();
                return;
            }
            
            long partSize = (endByte - startByte) + 1;
            System.out.println(" >> Thread " + clientAddr + " requesting " + fileToSend.getAbsolutePath() + " (Bytes: " + startByte + " to " + endByte + ", Size: " + partSize + " bytes, Mode: " + mode + ")");

            oout.writeLong(partSize);
            oout.flush();

            FileInputStream fis = new FileInputStream(fileToSend);
            OutputStream os = client.getOutputStream();
            
            if (mode == CopyMode.COPY_MULTITHREAD) {
                fis.skip(startByte);
                jio.partialCopyTransfer(fis, os, partSize);
                
            } else if (mode == CopyMode.ZEROCOPY_MULTITHREAD) {
                FileChannel fileChannel = fis.getChannel();
                WritableByteChannel wbc = Channels.newChannel(os);
                jio.partialZeroCopyTransfer(fileChannel, wbc, startByte, partSize); 
                fileChannel.close();
            }
            
            fis.close();
            System.out.println(" >> Thread " + clientAddr + " finished.");
        }catch(Exception e){
            System.err.println("Error in File Request: " + e.getCause());
        }
    }
}
