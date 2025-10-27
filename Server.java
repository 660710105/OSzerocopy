import java.io.*;
import java.net.*;
import java.util.Arrays;

class Server implements Runnable{
    private File fileDir;
    private int port;

    public Server(String pathFile, int port){
        this.fileDir = (pathFile.isEmpty())?
            new File("./send"):
            new File(pathFile);
        this.port = (port == 0)?
            9090:
            port;
    }

    @Override
    public void run() {
        if (!fileDir.exists() || !fileDir.isDirectory()) {
            throw new IllegalArgumentException("Directory does not exist: " + fileDir.getAbsolutePath());
        }
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("=== Server listening on port " + port + ", fileDirectory: " + fileDir.getAbsolutePath() + " ===");
            while (true) {
                try {
                    Socket client = serverSocket.accept();
                    System.out.println(" >> User "+client.getLocalSocketAddress() + " has connected.");
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
            
            System.out.println(" >> Sent " + file.getName() + " filenames to client " + client.getRemoteSocketAddress());

        } catch (IOException e) {
            System.err.println("Error sending file list to client " + client.getRemoteSocketAddress() + ": " + e.getMessage());
        } finally {
            try {
                client.close();
                System.out.println(" >> Client " + client.getRemoteSocketAddress() + " disconnected.");
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }
}