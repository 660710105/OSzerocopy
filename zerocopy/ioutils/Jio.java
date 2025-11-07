package zerocopy.ioutils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

public class Jio {
    private static int BUFFER_SIZE = 64 * 1024; // 64 KB

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

    public void bufferCopyThread(File file, FileChannel fileChannel, WritableByteChannel wbc) throws IOException {

    }

    public String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) { // Ensure dot is not at the start or end
            return filename.substring(dotIndex + 1);
        }
        return null; // No extension found
    }
}
