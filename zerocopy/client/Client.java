package zerocopy.client;

import java.io.*;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Scanner;

import zerocopy.common.CopyMode;
import zerocopy.fileutils.FileList;
import zerocopy.fileutils.FileflyClient;
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
        try {
            Socket socket = new Socket(host, port);
            socket.setKeepAlive(true);

            InputStream socketInputStream = socket.getInputStream();
            OutputStream socketOutputStream = socket.getOutputStream();
            
            ObjectOutputStream oout = new ObjectOutputStream(socketOutputStream);
            ObjectInputStream oin = new ObjectInputStream(socketInputStream);

            ReadableByteChannel rbc = Channels.newChannel(socketInputStream);

            oout.writeObject("CLIENT_REQUEST");
            oout.flush();

            @SuppressWarnings("unchecked")
                ArrayList<FileList> _rawFileList = (ArrayList<FileList>) oin.readObject();
            
            FileflyClient filefly = new FileflyClient(_rawFileList);
            
            System.out.println("=== List files ===");
            System.out.println(filefly.fancyFileInfo());

            System.out.println("=== Select File ===");
            System.out.print("Enter index of file: ");
            int fileIdx = Integer.parseInt(sc.nextLine()) - 1;
            
            if (fileIdx < 0 || fileIdx > filefly.size()) {
                System.err.println("Error: File index out of bound");
            }

            System.out.println("=== Select Mode ===\n"
                               + "1 = Copy \n"
                               + "2 = Zero-Copy \n"
                               + "3 = Copy-MultiThreads \n"
                               + "4 = Zero-Copy-MultiThreads");
            
            System.out.print("Select: ");
            
            int modeIdx = sc.nextInt();
            if (modeIdx < 1 || modeIdx > 4) {
                System.err.println("Error: Mode not found");
            }
            
            CopyMode mode = CopyMode.fromMode(modeIdx);
            
            oout.writeInt(fileIdx);
            oout.writeObject(mode);

            int nthOfThread = 1;
            if (mode == CopyMode.COPY_MULTITHREAD || mode == CopyMode.ZEROCOPY_MULTITHREAD) {
                System.out.print("Number of Threads: ");
                nthOfThread = sc.nextInt();
                nthOfThread = (nthOfThread < 1)? 1:nthOfThread;
                oout.writeInt(nthOfThread);
            }
            oout.flush();

            long fileSizeByte = filefly.getFileList(fileIdx).getFileSizeByte();
            Size fileSize = filefly.getFileList(fileIdx).getFileSize();
            Size _highestFileSize = SizeConverter.toHighestSize(fileSize);
            
            System.out.printf(" >> Server will send " + _highestFileSize.toString() + ".\n");

            String _selectFileName = filefly.getFileList(fileIdx).getFileName();

            // create a new one on client
            File outFile = new File(targetDir, _selectFileName);
            FileOutputStream fos = new FileOutputStream(outFile);
            
            PrintProcess printProcess = new PrintProcess();
            Thread processThread = new Thread(printProcess);
            
            long startTime = System.currentTimeMillis();
            
            byte[] buffer = new byte[Jio.BUFFER_SIZE];
            long remainSize = fileSizeByte;
            
            switch (mode) {
            case COPY:
                processThread.start();
                while (remainSize > 0) {
                    int neededToRead = (int) Math.min(buffer.length, remainSize);
                    int readByte = socketInputStream.read(buffer, 0, neededToRead);
                    
                    if (readByte < 0) {
                        throw new EOFException("Unexpected EOF");
                    }
                    
                    fos.write(buffer, 0, readByte);
                    remainSize -= readByte;

                    printProcess.setProcess(fileSizeByte - remainSize);
                }
                processThread.interrupt();
                printProcess.stop();
                fos.flush();
                break;
            case ZEROCOPY:
                long position = 0;
                System.out.println("Downloading file ...");
                while (position < fileSizeByte) {
                    long transferred = fos.getChannel().transferFrom(rbc, position, fileSizeByte);
                    if (transferred <= 0) {
                        System.err.println("Transfer stalled or socket closed.");
                        break;
                    }
                    position += transferred;
                }
                break;
            case COPY_MULTITHREAD:
            case ZEROCOPY_MULTITHREAD:
                try {
                    MultithreadReceive multithreadReceive = new MultithreadReceive();
                    multithreadReceive.multiThread(outFile, fileSizeByte, host, port, nthOfThread, mode, targetDir);
                } catch (InterruptedException e) {
                    System.err.println("Multi-thread download interrupted.");
                    e.printStackTrace();
                }
                break;
            }

            long endTime = System.currentTimeMillis();
            double totalTime = (endTime - startTime) / 1000F;
            String totalTimeString = String.format("%.2f", totalTime);

            System.out.printf("Completely received file: " + _selectFileName +
                              " with mode: " + mode.toString() +
                              " done in " + totalTimeString + "s\n");

            fos.close();
            
            oout.writeBoolean(true); // send complete
            oout.flush();

            // wait for client fully terminate
            Thread.sleep(2000);
            
            oin.close();
            oout.close();

            // do not need since it is already close, but for good will
            socketInputStream.close();
            socketOutputStream.close();
            socket.close();
        } catch (SocketException s) {
            System.out.println("Disconnected server");
            s.printStackTrace();
        } catch (IOException i) {
            i.printStackTrace();
        } catch (ClassNotFoundException c) {
            c.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            sc.close();
        }
    }
}
