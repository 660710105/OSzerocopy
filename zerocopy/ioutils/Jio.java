package zerocopy.ioutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

import zerocopy.common.CopyMode;

public class Jio {
    public static int BUFFER_SIZE = 64 * 1024;

    public void copyTransfer(InputStream fileIn, OutputStream socketOut) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = fileIn.read(buffer)) != -1) {
            socketOut.write(buffer, 0, bytesRead);
        }
        socketOut.flush();
    }

    public void zeroCopyTransfer(FileChannel fileChannel, WritableByteChannel wbc, long fileSizeByte) throws IOException {
        try {
            long position = 0;
            long fileSize = fileSizeByte;
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

    public void partialCopyTransfer(InputStream fileIn, OutputStream socketOut, long partSize) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        long remaining = partSize;
        
        while (remaining > 0 && (bytesRead = fileIn.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
            socketOut.write(buffer, 0, bytesRead);
            remaining -= bytesRead;
        }
        socketOut.flush();
    }

    public void partialZeroCopyTransfer(FileChannel fileChannel, WritableByteChannel wbc, long startPosition, long partSize) throws IOException {
        try {
            long position = startPosition;
            long remainingSize = partSize;
            while (remainingSize > 0) {
                long transferred = fileChannel.transferTo(position, remainingSize, wbc);
                if (transferred <= 0 && remainingSize > 0) {
                     System.err.println("Partial transfer stalled or socket closed.");
                    break;
                }
                position += transferred;
                remainingSize -= transferred;
            }
        } catch (SocketException s) {
           System.err.println("Socket exception during partial zero-copy: " + s.getMessage());
        }
    }
}
