import java.io.*;
import java.net.*;
import java.util.Arrays;

public class Server {
    private static File fileDir;
    private static int port;

    public static void main(String args[]) {
        try {
            if (args.length < 1) {
                System.out.println("Usage: java Server <fileDir> [port]");
                return;
            }
            fileDir = new File(args[0]);
            if (args.length >= 2) {
                port = Integer.parseInt(args[2]);
            } else {
                port = 9090;
            }
            Server server = new Server();
            server.start();

        } catch (IOException i) {
            // IOE throws Exception
        }
    }

    public void start() throws IOException {
        if (!fileDir.exists() || !fileDir.isDirectory()) {
            throw new IllegalArgumentException("Directory does not exist: " + fileDir.getAbsolutePath());
        }
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server listening on port " + port + ", fileDirectory: " + fileDir.getAbsolutePath());
            while (true) {
                try {
                    Socket client = serverSocket.accept();
                    System.out.println("=== User "+client.getLocalSocketAddress() + " has connected. ===");
                    sendFilenameList(client, fileDir);
                } catch (Exception e) {
                    serverSocket.close();
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendFilenameList(Socket client, File file) {
        try (
            OutputStream output = client.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
        ) {

            writer.println(file);
            
            System.out.println("Sent " + file.getName() + " filenames to client " + client.getRemoteSocketAddress());

        } catch (IOException e) {
            System.err.println("Error sending file list to client " + client.getRemoteSocketAddress() + ": " + e.getMessage());
        } finally {
            try {
                client.close();
                System.out.println("Client " + client.getRemoteSocketAddress() + " disconnected.");
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }
}