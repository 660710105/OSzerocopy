import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

public class ZeroCopyFileTransfer {

    private static final int PORT = 9090;
    private static final String FILE_NAME = "test_data.txt";
    private static final int FILE_SIZE_MB = 100;
    private static final int BUFFER_SIZE = 8 * 1024 * 1024;

    public static void main(String[] args) throws Exception {
        System.out.println("--- Java Network Zero-Copy Demo ---");
        File testFile = new File(FILE_NAME);
        createDummyFile(testFile, FILE_SIZE_MB);
        Thread serverThread = new Thread(() -> {
            try {
                startServer(testFile.length());
            } catch (IOException e) {
                System.err.println("Server error: " + e.getMessage());
            }
        });
        serverThread.start();

        Thread.sleep(1000);

        runTest(testFile, "Traditional", true);
        runTest(testFile, "Zero-Copy", false);
        serverThread.interrupt();
        testFile.delete();
    }

    private static void runTest(File file, String name, boolean useTraditionalIO) throws IOException {
        System.out.printf("\n--- Running %s Test (%d MB) ---\n", name, FILE_SIZE_MB);
        long startTime = System.nanoTime();

        try (Socket socket = new Socket("localhost", PORT);
             FileInputStream fileIn = new FileInputStream(file)) {

            if (useTraditionalIO) {
                traditionalTransfer(fileIn, socket.getOutputStream());
            } else {
                zeroCopyTransfer(fileIn.getChannel(), socket.getChannel());
            }

        } catch (IOException e) {
            System.err.println(name + " Client Error: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        long durationTime = endTime - startTime;
        System.out.printf("%s Transfer Time: %.2f ms\n", name, durationTime);
    }

    private static void traditionalTransfer(InputStream fileIn, OutputStream socketOut) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = fileIn.read(buffer)) != -1) {
            socketOut.write(buffer, 0, bytesRead);
        }
        socketOut.flush();
    }

    private static void zeroCopyTransfer(FileChannel fileChannel, SocketChannel socketChannel) throws IOException {
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

    private static void startServer(long fileLength) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server listening on port " + PORT + "...");

            Socket client1 = serverSocket.accept();
            System.out.println("Server: Client 1 connected for Traditional test.");
            receiveFile(client1.getInputStream(), fileLength, "Traditional");
            client1.close();

            Socket client2 = serverSocket.accept();
            System.out.println("Server: Client 2 connected for Zero-Copy test.");
            receiveFile(client2.getInputStream(), fileLength, "Zero-Copy");
            client2.close();

        } catch (Exception e) {
            if (!Thread.currentThread().isInterrupted()) {
                throw e;
            }
        }
    }

    private static void receiveFile(InputStream socketIn, long expectedLength, String testName) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long totalReceived = 0;
        int bytesRead;

        while ((bytesRead = socketIn.read(buffer)) != -1) {
            totalReceived += bytesRead;
            if (totalReceived >= expectedLength && expectedLength > 0) {
                if (totalReceived == expectedLength) {
                    break;
                }
            }
        }
        System.out.printf("Server: %s received %d bytes.\n", testName, totalReceived);
    }

    private static void createDummyFile(File file, int sizeMB) throws IOException {
        System.out.printf("Creating dummy file: %s (%d MB)...", file.getName(), sizeMB);
        long fileSize = (long) sizeMB * 1024 * 1024;
        byte[] zeroByte = new byte[]{'0'};
        try (FileOutputStream fos = new FileOutputStream(file)) {
            for (long i = 0; i < fileSize; i++) {
                fos.write(zeroByte);
            }
        } catch (Exception e){
            System.out.println("error");
        }
        System.out.println(" Done.");
    }
}
