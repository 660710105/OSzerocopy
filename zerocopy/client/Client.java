package zerocopy.client;

import java.io.*;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.Scanner;

import zerocopy.fileutils.Filefly;
import zerocopy.ioutils.Jio;
import zerocopy.ioutils.PrintProcess;
import zerocopy.ioutils.notation.Size;
import zerocopy.ioutils.notation.SizeConverter;
import zerocopy.ioutils.notation.SizeNotation;

public class Client{
    private String host;
    private int port;
    private File targetDir;

    public Client(String host, int port, String pathFile) {
        this.host = host;
        this.port = port;
        this.targetDir = new File(pathFile);

        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
    }

    public void run() {
        Scanner sc = new Scanner(System.in);
        Socket socket;
        ObjectInputStream oin;
        ObjectOutputStream oout;

        try {
            System.out.println(" >> Client download at " + targetDir);

            socket = new Socket(host, port);
            socket.setKeepAlive(true);

            System.out.println(" >> Connected Server " + host + ": " + port);

            oout = new ObjectOutputStream(socket.getOutputStream());
            oin = new ObjectInputStream(socket.getInputStream());

            Object _rawFilefly = oin.readObject();
            Filefly filefly = (Filefly) _rawFilefly;
            
            System.out.print("=== List files ===\n" + filefly.fancyFileInfo());

            System.out.println("=== Select File ===");
            System.out.print("Enter index of file: ");
            int fileIdx = Integer.parseInt(sc.nextLine());
            
            if (fileIdx >= filefly.size()) {
                System.err.println("Error: File index out of bound");
            }

            System.out.println("=== Select Mode ===\n"
                               + "0 = Copy \n"
                               + "1 = Zero-Copy \n"
                               + "2 = Copy-MultiThreads \n"
                               + "3 = Zero-Copy-MultiThreads");
            
            System.out.print("Select: ");
            
            int modeIdx = sc.nextInt();
            if (modeIdx > 3) {
                System.err.println("Error: Mode not found");
            }
            
            String mode = "";
            switch (modeIdx) {
            case 0:
                mode = "Copy";
                break;
            case 1:
                mode = "Zero-Copy";
                break;
            case 2:
                mode = "Copy-MultiThreads";
                break;
            case 3:
                mode = "Zero-Copy-MultiThreads";
                break;
            }

            oout.writeInt(fileIdx);
            oout.writeInt(modeIdx);
            oout.flush();

            long fileSize = oin.readLong();
            System.out.printf(" >> Server will send "
                              + SizeConverter.toHighestSize(new Size(SizeNotation.B, fileSize)).toString()
                              + ".\n");

            File outFile = new File(targetDir, filefly.getFile(fileIdx).getName());
            FileOutputStream fos = new FileOutputStream(outFile);
            long start = System.currentTimeMillis();
            InputStream in = socket.getInputStream();
            ReadableByteChannel rbc = Channels.newChannel(in);

            byte[] buffer = new byte[Jio.BUFFER_SIZE];
            long remain = fileSize;
            PrintProcess printProcess = new PrintProcess();
            Thread processThread = new Thread(printProcess);
            switch (mode) {
            case "Copy":
                processThread.start();
                while (remain > 0) {
                    int toRead = (int) Math.min(buffer.length, remain);
                    int r = in.read(buffer, 0, toRead);
                    if (r < 0)
                        throw new EOFException("Unexpected EOF");
                    fos.write(buffer, 0, r);
                    remain -= r;

                    printProcess.setProcess(fileSize-remain);
                }
                processThread.interrupt();
                printProcess.stop();
                fos.flush();

                break;
            case "Zero-Copy":
                long position = 0;
                System.out.println("Downloading file ...");
                while (position < fileSize) {
                                                
                    long transferred = fos.getChannel().transferFrom(rbc, position, fileSize);

                    if (transferred <= 0) {
                        System.err.println("Transfer stalled or socket closed.");
                        break;
                    }

                    position += transferred;
                }
                break;
            case "Copy-MultiThreads":
            case "Zero-Copy-MultiThreads":

                break;
            }

            long end = System.currentTimeMillis();
            System.out.printf("Sent " + filefly.getFile(fileIdx).getName()
                              + ", mode " + mode
                              + " (%.2f s)\n", ((end - start) / 1000.0));
            oout.writeBoolean(true); // send complete
            oout.flush();
            Thread.sleep(2000);
            oin.close();
            oout.close();
            in.close();
            fos.close();
            socket.close();
        } catch (SocketException s) {
            System.out.println("Disconnected server");
            s.printStackTrace();
        } catch (IOException i) {
            i.printStackTrace();
        } catch (ClassNotFoundException c) {
            c.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            sc.close();
        }
    }
}
