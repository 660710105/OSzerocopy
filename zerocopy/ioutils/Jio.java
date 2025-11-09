package zerocopy.ioutils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

public class Jio {
    private static int BUFFER_SIZE = 64 * 1024;

    public void copyTransfer(File file, InputStream fileIn, OutputStream socketOut) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = fileIn.read(buffer)) != -1) {
            socketOut.write(buffer, 0, bytesRead);
        }
        socketOut.flush();
    }

    public void zeroCopyTransfer(File file, FileChannel fileChannel, WritableByteChannel wbc) throws IOException {
        try {
            long fileSize = file.length();
            long position = 0;
            while (position < fileSize) {
                long transferred = fileChannel.transferTo(position, fileSize - position, wbc);

                if (transferred <= 0) {
                    System.err.println("Transfer stalled or socket closed.");
                    break;
                }

                position += transferred;
            }
        } catch (SocketException s) {
           s.printStackTrace();
        }
    }

    public void multiThread(File file, String host, int port, int nthread, String mode) throws IOException {
        long partSize = file.length() / nthread;
        Thread[] threads = new Thread[nthread];
        for (int i = 0; i < nthread; i++) {
            long startByte = i * partSize;
            long endByte;

            if (i == nthread - 1) {
                endByte = nthread - 1;
            } else {
                endByte = startByte + partSize - 1;
            }

            String tempPartFile = "part." + i;
            SendThread worker = new SendThread(file, host, port, startByte, endByte, tempPartFile);
            threads[i] = new Thread(worker);
            threads[i].start();
        }
    }
}