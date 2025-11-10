package zerocopy.ioutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

import zerocopy.common.CopyMode;

public class Jio {
    public static int BUFFER_SIZE = 64 * 1024;

    public void copyTransfer(InputStream fileIn, OutputStream socketOut) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = fileIn.read(buffer)) != -1) {
            socketOut.write(buffer, 0, bytesRead);
        }
        socketOut.flush();
    }

    public void zeroCopyTransfer(File file, FileChannel fileChannel, WritableByteChannel wbc) throws IOException {
        try {
            long fileSize = file.length();
            long position = 0;
            while (position < fileSize) {
                long transferred = fileChannel.transferTo(position, fileSize - position, wbc);

                if (transferred <= 0) {
                    System.err.println("Transfer stalled or socket closed.");
                    break;
                }

                position += transferred;
            }
        } catch (SocketException s) {
            s.printStackTrace();
        }
    }

    public void partialCopyTransfer(InputStream fileIn, OutputStream socketOut, long partSize) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        long remaining = partSize;
        
        while (remaining > 0 && (bytesRead = fileIn.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
            socketOut.write(buffer, 0, bytesRead);
            remaining -= bytesRead;
        }
        socketOut.flush();
    }

    public void partialZeroCopyTransfer(FileChannel fileChannel, WritableByteChannel wbc, long startPosition, long partSize) throws IOException {
        try {
            long position = startPosition;
            long count = partSize;
            while (count > 0) {
                long transferred = fileChannel.transferTo(position, count, wbc);
                if (transferred <= 0 && count > 0) {
                     System.err.println("Partial transfer stalled or socket closed.");
                    break;
                }
                position += transferred;
                count -= transferred;
            }
        } catch (SocketException s) {
           System.err.println("Socket exception during partial zero-copy: " + s.getMessage());
        }
    }

    public void multiThread(File outFile, long fileSize,String host, int port, int nthread, CopyMode mode, File targetDir) throws IOException, InterruptedException {
        long partSize = fileSize / nthread;
        
        Thread[] threads = new Thread[nthread];
        File[] partFiles = new File[nthread];

        System.out.println("Starting " + nthread + " threads, downloading to temporary part-files...");

        for (int i = 0; i < nthread; i++) {
            long startByte = i * partSize;
            long endByte;

            if (i == nthread - 1) {
                endByte = fileSize - 1;
            } else {
                endByte = startByte + partSize - 1;
            }

            String tempPartName = outFile.getName() + ".part." + i;
            partFiles[i] = new File(targetDir, tempPartName);
            
            SendThread worker = new SendThread(i, outFile, host, port, startByte, endByte,partFiles[i].getAbsolutePath() , mode);
            threads[i] = new Thread(worker);
            threads[i].start();
        }

        for (int i = 0; i < nthread; i++) {
            threads[i].join();
        }

        System.out.println("All threads finished writing.");

        try (FileOutputStream fos = new FileOutputStream(outFile);
             FileChannel destChannel = fos.getChannel()) {
            
            for (int i = 0; i < nthread; i++) {
                if (!partFiles[i].exists()) {
                    System.err.println("Error: Missing part file: " + partFiles[i].getName());
                    continue;
                }
                try (FileInputStream fis = new FileInputStream(partFiles[i]);
                     FileChannel srcChannel = fis.getChannel()) {
                    
                    System.out.println("Appending " + partFiles[i].getName());
                    srcChannel.transferTo(0, srcChannel.size(), destChannel);
                }

                partFiles[i].delete();
            }
        }
        System.out.println("File reassembly complete.");
    }
}
