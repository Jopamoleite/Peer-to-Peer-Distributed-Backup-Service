package project.store;

import project.protocols.DeleteProtocol;

import java.io.*;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * class that keeps record of the conversion of file_name to file_id
 */
public class FilesListing implements Serializable {

    private static FilesListing filesListing = new FilesListing();
    private ConcurrentHashMap<String, FileInfo> files = new ConcurrentHashMap<>();

    //singleton
    private FilesListing() {  }

    /**
     * get all files listed
     * @return an instance FilesListing
     */
    public static FilesListing getInstance() {
        return filesListing;
    }

    public static void setInstance(FilesListing fl) { filesListing = fl; }

    public String getFileId(String file_name) {
        return files.get(file_name).getFileId();
    }

    public int getNumberOfChunks(String file_name) {
        return files.get(file_name).getNumberOfChunks();
    }

    public String getFileName(String file_id) {
        Iterator it = files.entrySet().iterator();

        while(it.hasNext()){
            ConcurrentHashMap.Entry file = (ConcurrentHashMap.Entry) it.next();
            FileInfo info = (FileInfo) file.getValue();
            if(file_id.equals(info.getFileId())){
                return (String) file.getKey();
            }
        }
        return null;
    }

    public String getFilePath(String file_id) {
        Iterator it = files.entrySet().iterator();

        while(it.hasNext()){
            ConcurrentHashMap.Entry file = (ConcurrentHashMap.Entry) it.next();
            FileInfo info = (FileInfo) file.getValue();
            if(file_id.equals(info.getFileId())){
                return info.getFilePath();
            }
        }
        return null;
    }

    public boolean addFile(String file_name, String file_id, Integer number_of_chunks, String file_path) {

        //put returns the previous value associated with key, or null if there was no mapping for key
        FileInfo file = files.put(file_name, new FileInfo(file_name, file_id, number_of_chunks, file_path));

        if (file != null) {
            //backing up a file with the same name that wasn't change
            if(!file.getFileId().equals(file_id)) {
                System.out.println("This file_name already exists, the content will be updated.");

                //deletes the older file
                System.out.println("Deleting " + file.getNumberOfChunks() + " chunks from the out of date file");

                //deletes file from network storage
                DeleteProtocol.processDelete(file_id);

                //deletes own files with chunks of the file in the 3 folders ( files, stored, restored)
                FileManager.deleteFilesFolders(file.getFileId());

                //old file is ours so unregister chunks of the file
                Store.getInstance().removeStoredChunks(file.getFileId());

                return true;

            } else {
                return  false;
            }
        }
        return true;
    }

    public void deleteFileRecords(String file_name) {
        files.remove(file_name);
    }

    public ConcurrentHashMap<String, FileInfo> getFiles() {
        return files;
    }
}
