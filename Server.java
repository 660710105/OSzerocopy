import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

class Server implements Runnable {
    private File fileDir;
    private int port;

    public Server(String pathFile, int port) {
        this.fileDir = (pathFile.isEmpty()) ? new File("./send") : new File(pathFile);
        this.port = (port == 0) ? 9090 : port;
    }

    @Override
    public void run() {
        if (!fileDir.exists() || !fileDir.isDirectory()) {
            throw new IllegalArgumentException("Directory does not exist: " + fileDir.getAbsolutePath());
        }
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println(
                    "=== Server listening on port " + port + ", fileDirectory: " + fileDir.getAbsolutePath() + " ===");
            while (true) {
                Socket client = serverSocket.accept();
		client.setKeepAlive(true);
                try {
                    System.out.println(" >> Client " + client.getRemoteSocketAddress() + " has connected.");
		    
                    ObjectOutputStream oout = new ObjectOutputStream(client.getOutputStream());
                    sendFilenameList(oout, fileDir);

                    ObjectInputStream oin = new ObjectInputStream(client.getInputStream());

                    Object name = oin.readObject();
                    if (!(name instanceof String)) {
                        System.err.println("Error: expected filename string");
                        continue;
                    }
                    String filename = (String) name;

                    Object mode = oin.readObject();
                    if (!(mode instanceof String)) {
                        System.err.println("Error: expected mode number");
                        continue;
                    }
                    String modeNumber = (String) mode;

                    File fileToSend = new File(fileDir, filename);
                    if (!fileToSend.exists() || !fileToSend.isFile()) {
                        oout.writeBoolean(false);
                        oout.flush();
                        System.out.println(" >> Client " + client.getRemoteSocketAddress() +" requested missing file: " + filename);
                        continue;
                    } else {
			    oout.writeBoolean(true);
			    oout.flush();
		    }

                    Jio jio = new Jio();
                    FileInputStream fis = new FileInputStream(new File(filename));
                    switch (modeNumber) {
                        case "0":
                            jio.copyTransfer(fis, client.getOutputStream());
                            break;
                        case "1":
                            jio.zeroCopyTransfer(fis.getChannel(), client.getChannel());
                            break;
                        case "2":
                            jio.bufferCopyThread(fis.getChannel(), client.getChannel());
                        default:
                            break;
                    }

                    

                } catch (SocketException s) {
			System.err.println("err: " + s.getCause());
                    System.err.println(" >> Client " + client.getRemoteSocketAddress() +" disconnected.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendFilenameList(ObjectOutputStream oout, File file) {
        try {
		//ObjectOutputStream oout = new ObjectOutputStream(client.getOutputStream());

		//System.out.println(" >> Sent list of files to client " + client.getRemoteSocketAddress());

            File[] files = file.listFiles();
            List<String> listFileName = new ArrayList<>();
            for (File f : files) {
                if (f instanceof File)
                    listFileName.add(f.getName());
            }
            oout.writeObject(listFileName);
            oout.flush();
        } catch (IOException e) {
            // System.err.println(
            //         "Error sending file list to client " + client.getRemoteSocketAddress() + ": " + e.getMessage());
        }
    }
}
