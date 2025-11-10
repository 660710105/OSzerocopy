package zerocopy.fileutils;

import java.io.File;
import java.util.ArrayList;

import zerocopy.ioutils.notation.Size;
import zerocopy.ioutils.notation.SizeConverter;

public class FileflyClient {
    private ArrayList<FileList> files = new ArrayList<FileList>();
    
    public FileflyClient(ArrayList<FileList> files) {
        this.files = files;
    }

    public FileList getFileList(int idx) {
        return files.get(idx);
    }

    public FileList[] getAllFilesList() throws Exception {
        Object[] rawFileListArray = files.toArray();
        if (rawFileListArray instanceof File[]) {
            return (FileList[]) rawFileListArray;
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
        for (FileList _fileList : files) {
            stringBuilder.append(fileIdx++ + ". ");
            
            String fileName = _fileList.getFileName();
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

            Size _rawFileSize = _fileList.getFileSize();
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
