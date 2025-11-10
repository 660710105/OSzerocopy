package zerocopy.server;

import java.io.*;
import java.net.*;

import zerocopy.fileutils.Filefly;

public class Server{
        private Filefly filefly;
        private int port;

        public Server(String pathFile, int port) {
                this.filefly = new Filefly(pathFile);
                this.port = port;
        }

        public void run() {
                try {
                        ServerSocket serverSocket = new ServerSocket(port);
                        System.out.println(
                                        "=== Server listening on port " + port + ", fileDirectory: "
                                                        + filefly.getOwnFile().getAbsolutePath() + " ===");
                        while (true) {
                                Socket client = serverSocket.accept();
                                Thread handle = new Thread(new HandlerClient(client, filefly));
                                handle.start();
                        }
                } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                }
        }
}
