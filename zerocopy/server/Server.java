package zerocopy.server;

import java.io.*;
import java.net.*;

import zerocopy.fileutils.FileflyServer;

public class Server{
    private FileflyServer fileflyServer;
    private int port;
    private String pathFile;

    public Server(String pathFile, int port) {
        this.pathFile = pathFile;
        this.fileflyServer = new FileflyServer(pathFile);
        this.port = port;
    }

    public void run() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("=== Server listening on port " + port +
                               ", fileDirectory: " + pathFile + " ===");
            
            while (true) {
                Socket client = serverSocket.accept();
                Thread handle = new Thread(new HandlerClient(client, fileflyServer));
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
