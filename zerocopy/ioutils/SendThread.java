package zerocopy.ioutils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SendThread implements Runnable {
    private String host, filename, partFile;
    private int port;
    private long startByte, endByte;

    public SendThread(File file, String host, int port, long startByte, long endByte, String partFile) {
        this.filename = file.getName();
        this.startByte = startByte;
        this.endByte = endByte;
        this.partFile = partFile;
    }

    @Override
    public void run() {
        try {
            Socket socket = new Socket(host, port);
            ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream());
            InputStream in = socket.getInputStream();
            FileOutputStream fos = new FileOutputStream(partFile);
            String request = socket.getRemoteSocketAddress() + "get " + filename + ": " + startByte + " : " + endByte;
            oout.writeBytes(request);
            oout.flush();
            System.out.println("Thread [" + partFile + "] requesting: " + request);

            byte[] buffer = new byte[64 * 1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) > 0) {
                fos.write(buffer, 0, bytesRead);
            }
            System.out.println("Thread [" + partFile+ "] finished.");

        } catch (IOException e) {
            System.err.println("Error in Thread " + partFile + ": " + e.getMessage());
        }
    }
}