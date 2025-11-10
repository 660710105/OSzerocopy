package zerocopy.fileutils;

import java.io.File;
import java.util.ArrayList;

public class FileflyServer {
    private ArrayList<File> files = new ArrayList<File>();
    private File ownFile;
    public FileflyServer(String path) {
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

    public ArrayList<FileList> convertToFileLists() {
        ArrayList<FileList> fileLists = new ArrayList<>();
        for (File _file : files) {
            String fileName = _file.getName();
            long fileSizeByte = _file.length();
            fileLists.add(new FileList(fileName, fileSizeByte));
        }
        return fileLists;
    }

    public int size() {
        return files.size();
    }
}
