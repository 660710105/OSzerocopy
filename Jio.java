import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

public class Jio {
    private static int BUFFER_SIZE = 8 * 1024;
    public void copyTransfer(InputStream fileIn, OutputStream socketOut) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = fileIn.read(buffer)) != -1) {
            socketOut.write(buffer, 0, bytesRead);
        }
        socketOut.flush();
    }

    public void zeroCopyTransfer(FileChannel fileChannel, SocketChannel socketChannel) throws IOException {
        long bytesToSend = fileChannel.size();
        long bytesSent = 0;
        long position = 0;

        while (bytesSent < bytesToSend) {
            long count = bytesToSend - bytesSent;
            long transferred = fileChannel.transferTo(position, count, socketChannel);
            if (transferred <= 0) {
                break;
            }
            bytesSent += transferred;
            position += transferred;
        }
    }

    public void bufferCopyThread(FileChannel fileChannel, SocketChannel socketChannel) throws IOException{

    }

    public static String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) { // Ensure dot is not at the start or end
            return filename.substring(dotIndex + 1);
        }
        return null; // No extension found
    }
}