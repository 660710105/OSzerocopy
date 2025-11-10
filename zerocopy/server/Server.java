package zerocopy.server;

import java.io.*;
import java.net.*;

public class Server implements Runnable {
        private File fileDir;
        private int port;

        public Server(String pathFile, int port) {
                this.fileDir = new File(pathFile);
                this.port = port;
        }

        @Override
        public void run() {
                if (!fileDir.exists() || !fileDir.isDirectory()) {
                        throw new IllegalArgumentException("Directory does not exist: " + fileDir.getAbsolutePath());
                }

                try (ServerSocket serverSocket = new ServerSocket(port)){
                        System.out.println(
                                        "=== Server listening on port " + port + ", fileDirectory: "
                                                        + fileDir.getAbsolutePath() + " ===");
                        while (true) {
                                Socket client = serverSocket.accept();
                                Thread handle = new Thread(new HandlerClient(client, fileDir));
                                handle.start();
                        }
                } catch (IOException e) {
                        e.printStackTrace();
                }
        }
}
