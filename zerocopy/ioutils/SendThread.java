package zerocopy.ioutils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import zerocopy.common.CopyMode;

public class SendThread implements Runnable {
    private String host, filename, partFile;
    private int port, orderThread;
    private long startByte, endByte;
    private CopyMode mode;
    private static int BUFFER_SIZE = 64 * 1024;

    public SendThread(int orderThread, File file, String host, int port, long startByte, long endByte, String partFile, CopyMode mode) {
        this.orderThread = orderThread;
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
        ObjectOutputStream oout = null;
        ObjectInputStream oin = null;
        InputStream in = null;

        try {
            socket = new Socket(host, port);

            oout = new ObjectOutputStream(socket.getOutputStream());
            oin = new ObjectInputStream(socket.getInputStream());
            in = socket.getInputStream();
            fos = new FileOutputStream(partFile);

            oout.writeObject("FILE_REQUEST");
            oout.flush();

            oout.writeObject(filename);
            oout.writeLong(startByte);
            oout.writeLong(endByte);
            oout.writeObject(mode);
            oout.flush();

            System.out.println("Thread [" + orderThread + "] requesting: " + partFile + " bytes " + startByte + " to " + endByte);

            long partSize = oin.readLong();
            if (partSize == -1) {
                System.err.println("Thread [" + orderThread + "] Error: Server could not find file.");
                return;
            }

            System.out.println("Thread [" + orderThread + "] writing to " + partFile + " (" + partSize + " bytes)");

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long remain = partSize;
            while (remain > 0 && (bytesRead = in.read(buffer, 0, (int) Math.min(remain, BUFFER_SIZE))) > 0) {
                fos.write(buffer, 0, bytesRead);
                remain -= bytesRead;
            }
            
            System.out.println("Thread [" + orderThread + "] finished.");


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { if (fos != null) fos.close(); } catch (IOException e) { e.printStackTrace(); }
            try { if (in != null) in.close(); } catch (IOException e) { e.printStackTrace(); }
            try { if (oin != null) oin.close(); } catch (IOException e) { e.printStackTrace(); }
            try { if (oout != null) oout.close(); } catch (IOException e) { e.printStackTrace(); }
            try { if (socket != null) socket.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }
}