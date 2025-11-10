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
                ServerSocket serverSocket = null;
                try {
                        serverSocket = new ServerSocket(port);
                        System.out.println(
                                        "=== Server listening on port " + port + ", fileDirectory: "
                                                        + filefly.getOwnFile().getAbsolutePath() + " ===");
                        while (true) {
                                Socket client = serverSocket.accept();
                                Thread handle = new Thread(new HandlerClient(client, filefly));
                                handle.start();
                        }
                } catch (IOException e) {
                        // e.printStackTrace();
                        System.err.println("Server socket error: " + e.getCause());
                } finally {
                        try { 
                                if(serverSocket != null) 
                                        serverSocket.close(); 
                        } catch (IOException io){
                                System.err.println("Server shutdown");
                        }
                }
        }
}
