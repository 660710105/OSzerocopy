import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class JioChannel {
    public static void main(String[] args) throws Exception {
        JioChannel channel = new JioChannel();
        String pathFile = args[0];
        String extension = getFileExtension(pathFile);
        String choice = args[1];
        
        try {
            if (args.length < 2) {
                System.out.println("usage JioChannel");
                return;
            }
            long start = System.currentTimeMillis();
            switch (choice) {
                case "c":
                    channel.copy(pathFile, extension);
                    break;
                case "z":
                    channel.zeroCopy(pathFile, extension);
                    break;
                default:
                    System.out.println("error method");
                    break;
            }
            long end = System.currentTimeMillis();
            long time = end - start;
            System.out.println("Time " + time + "ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void copy(String from, String to) throws IOException{
        byte[] data = new byte[8 * 1024];
        FileInputStream fis = null;
        FileOutputStream fos = null;
        long bytesToCopy = new File(from).length();
        long bytesCopied = 0;
        try {
            fis = new FileInputStream(from);
            fos = new FileOutputStream(to);
            
            while (bytesCopied < bytesToCopy){
                fis.read(data);
                fos.write(data);
                bytesCopied += data.length;
            }
            fos.flush();
        } finally {
            if(fis != null){
                fis.close();
            }
            if(fos != null){
                fos.close();
            }
            System.out.println("File copied");
        }
    }
    public void zeroCopy(String from, String to) throws IOException {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        FileChannel source = null;
        FileChannel destination = null;
        try {
            fis = new FileInputStream(from);
            fos = new FileOutputStream(to);
            source = fis.getChannel();
            destination = fos.getChannel();
            source.transferTo(0, source.size(), destination);
        } finally {
            if(fis != null){
                fis.close();
            }
            if(fos != null){
                fos.close();
            }
            System.out.println("File zero-copied");
        }
    }
    public static String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) { // Ensure dot is not at the start or end
            return filename.substring(dotIndex + 1);
        }
        return null; // No extension found
    }
}