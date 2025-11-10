package zerocopy.fileutils;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

import zerocopy.ioutils.notation.Size;
import zerocopy.ioutils.notation.SizeConverter;
import zerocopy.ioutils.notation.SizeNotation;

public class Filefly implements Serializable {
    private ArrayList<File> files = new ArrayList<File>();
    private File ownFile;
    
    public Filefly(String path) {
        ownFile = new File(path);
        if (ownFile.exists() == false) {
            throw new IllegalArgumentException("File or directory does not exist: " + ownFile.getAbsolutePath());
        }
        
        if (ownFile.isDirectory()) {
            File[] fileLists = ownFile.listFiles();
            for (File _file : fileLists) {
                files.add(_file);
            }
        } else {
            files.add(ownFile);
        }
    }

    public File getOwnFile() {
        return ownFile;
    }

    public File getFile(int idx) {
        return files.get(idx);
    }

    public File[] getAllFiles() throws Exception {
        Object[] rawFileArray = files.toArray();
        if (rawFileArray instanceof File[]) {
            return (File[]) rawFileArray;
        }
        throw new Exception("Cannot cast File object!");
    }

    public int size() {
        return files.size();
    }

    public String fancyFileInfo() {
        // if avg terminal size is 80x25 then filename 25 and 1 for spacing and 10 for size printing.
        int maxFileNamePrintLength = 25;
        int maxFileSizePrintLength = 10;

        StringBuilder stringBuilder = new StringBuilder();
        long fileIdx = 1;
        for (File _file : files) {
            stringBuilder.append(fileIdx++ + ". ");
            
            String fileName = _file.getName();
            if (fileName.length() > maxFileNamePrintLength) {
                // print ... at the end of file
                for (int i = 0; i < maxFileNamePrintLength - 3; i++) {
                    char c = fileName.charAt(i);
                    stringBuilder.append(c);
                }
                stringBuilder.append("...");
            } else {
                stringBuilder.append(fileName);
                for (int i = 0; i < maxFileNamePrintLength - fileName.length(); i++) {
                    stringBuilder.append(' ');
                }
            }
            stringBuilder.append(' '); // spacing

            long _rawFileSizeByte = _file.length();
            Size _rawFileSize = new Size(SizeNotation.B, _rawFileSizeByte);
            String fileSizeString = SizeConverter.toHighestSize(_rawFileSize).toString();

            // dejavu
            if (fileSizeString.length() > maxFileSizePrintLength) { 
                for (int i = 0; i < maxFileSizePrintLength - 3; i++) {
                    char c = fileSizeString.charAt(i);
                    stringBuilder.append(c);
                }
                stringBuilder.append("...");
            } else {
                stringBuilder.append(fileSizeString);
                for (int i = 0; i < maxFileSizePrintLength - fileSizeString.length(); i++) {
                    stringBuilder.append(' ');
                }
            }
            stringBuilder.append('\n');
        }
        return stringBuilder.toString();
    }
}
