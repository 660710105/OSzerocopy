package zerocopy.server;

import java.io.*;
import java.net.*;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

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

                try {
                        // 1. สร้าง ServerSocketChannel และผูกกับพอร์ต
                        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
                        serverSocketChannel.socket().bind(new InetSocketAddress(port));
                        serverSocketChannel.configureBlocking(true); // ทำงานแบบ Blocking เหมือนเดิม

                        System.out.println(
                            "=== Server listening on port " + port + ", fileDirectory: "
                                            + fileDir.getAbsolutePath() + " ===");

                        while (true) {
                                SocketChannel clientChannel = serverSocketChannel.accept(); // 2. รับ SocketChannel
                                Socket clientSocket = clientChannel.socket(); // 3. เอา Socket (แบบเก่า) ออกมา

                        // 4. ส่ง Socket (แบบเก่า) ไปให้ HandlerClient
                        // (HandlerClient จะดึง Channel กลับไปเอง)
                                Thread handle = new Thread(new HandlerClient(clientSocket, fileDir)); 
                                handle.start();
                        }
                } catch (IOException e) {
                        e.printStackTrace();
                }
        }
}
