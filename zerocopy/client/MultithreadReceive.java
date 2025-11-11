package zerocopy.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import zerocopy.common.CopyMode;
import zerocopy.ioutils.SendThread;

public class MultithreadReceive {
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
