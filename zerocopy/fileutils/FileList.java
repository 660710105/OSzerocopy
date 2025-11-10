package zerocopy.fileutils;

import java.io.Serializable;

import zerocopy.ioutils.notation.Size;
import zerocopy.ioutils.notation.SizeNotation;

public class FileList implements Serializable {
    private String fileName;
    private Size fileSize;
    private long fileSizeByte;
    
    public FileList(String fileName, long fileSizeByte) {
        this.fileName = fileName;
        this.fileSizeByte = fileSizeByte;
        this.fileSize = new Size(SizeNotation.B, fileSizeByte);
    }

    public String getFileName() {
        return fileName;
    }

    public Size getFileSize() {
        return fileSize;
    }

    public long getFileSizeByte() {
        return fileSizeByte;
    }
}
