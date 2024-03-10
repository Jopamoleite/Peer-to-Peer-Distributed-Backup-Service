package project.store;

import project.Macros;
import project.chunk.BackedupChunk;
import project.chunk.StoredChunks;
import project.peer.Peer;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Store implements Serializable {
    private static Store store = new Store();

    //state of others chunks
    private ConcurrentHashMap<String, StoredChunks> stored_chunks = new ConcurrentHashMap<>();

    //state of restored files - key file_id - value file_name
    private Hashtable<String, String> restored_files = new Hashtable<>();

    //state of our files - key file_id + chunk_no and value wanted_replication degree and list of peers
    private ConcurrentHashMap<String, BackedupChunk> backup_chunks = new ConcurrentHashMap<>();

    private String peer_directory_path;
    private String files_directory_path;
    private String store_directory_path;
    private String restored_directory_path;

    private long occupied_storage = 0; //in bytes
    private long storage_capacity = Macros.INITIAL_STORAGE;


    /**
     * creates the four needed directory
     */
    private Store() { }

    /**
     * creates the four needed directory
     */
    public void initializeStore(){
        //setting the directory name
        peer_directory_path = Peer.id + "_directory/";
        files_directory_path = peer_directory_path + "files/";
        store_directory_path = peer_directory_path + "stored/";
        restored_directory_path = peer_directory_path + "restored/";

        FileManager.createDirectory(peer_directory_path);
        FileManager.createDirectory(files_directory_path);
        FileManager.createDirectory(store_directory_path);
        FileManager.createDirectory(restored_directory_path);
    }

    public static Store getInstance(){
        return store;
    }

    public static void setInstance(Store storage) { store = storage; }


    //-------------------- STORAGE ------------------
    public long getStorageCapacity() {
        return storage_capacity;
    }

    public long getOccupiedStorage() {
        return occupied_storage;
    }

    public boolean hasSpace(Integer space_wanted) {
        return (this.storage_capacity >= this.occupied_storage + space_wanted);
    }
    /**
     * used when a new chunk is store ( by backup )
     * @param space_wanted storage space added
     */
    public synchronized void AddOccupiedStorage(long space_wanted) {
        occupied_storage += space_wanted;
    }

    /**
     * used when a chunk is deleted
     * @param occupied_space the amount of space in bytes used for storage
     */
    public void RemoveOccupiedStorage(long occupied_space) {
        occupied_storage -= occupied_space;
        if(occupied_storage < 0)
            occupied_storage = 0;
    }

    //---------------------------- BACKUP CHUNKS ----------------------------------

    public void newBackupChunk(String file_id, int chunk_no, int replication_degree) {
        String chunk_id = file_id + "_" + chunk_no;

        if(this.backup_chunks.containsKey(chunk_id)){
            BackedupChunk chunk = this.backup_chunks.get(chunk_id);

            chunk.setReplicationDegree(replication_degree);

            this.backup_chunks.replace(chunk_id, chunk);
        }
        else this.backup_chunks.put(chunk_id, new BackedupChunk(file_id, chunk_no, replication_degree));
    }

    public boolean addBackupChunks(String chunk_id, BigInteger key) {
        if(this.backup_chunks.containsKey(chunk_id)){
            BackedupChunk chunk = this.backup_chunks.get(chunk_id);

            if(chunk.getPeers().contains(key))
                return false;

            if(getFileActualReplicationDegree(chunk_id) >= getFileReplicationDegree(chunk_id))
                return true;

            chunk.addPeer(key);
            this.backup_chunks.replace(chunk_id, chunk);
        }
        return false;
    }

    public int getFileActualReplicationDegree(String chunk_id) {
        if(backup_chunks.containsKey(chunk_id))
            return backup_chunks.get(chunk_id).getPeers().size();
        else return -1;
    }

    public int getFileReplicationDegree(String chunk_id) {
        if(backup_chunks.containsKey(chunk_id))
            return backup_chunks.get(chunk_id).getReplicationDegree();
        else return -1;
    }

    public void removeBackupChunk(String chunk_id, BigInteger key){
        removeBackupChunk(chunk_id, key, true);
    }

    public void removeBackupChunk(String chunk_id, BigInteger key, boolean delete) {
        BackedupChunk chunk = this.backup_chunks.get(chunk_id);

        if(chunk != null ){
            chunk.removePeer(key);

            if(delete && chunk.getPeers().size() < 1)
                this.backup_chunks.remove(chunk_id);
            else{
                this.backup_chunks.replace(chunk_id, chunk);
            }
        }

    }

    public void removeBackupChunk(String chunk_id) {
        this.backup_chunks.remove(chunk_id);
    }

    public ArrayList<BigInteger> getBackupChunksPeers(String chunk_id) {
        return backup_chunks.get(chunk_id).getPeers();
    }

    public ArrayList<BackedupChunk> verifyBackupChunks(BigInteger key) {
        ArrayList<BackedupChunk> chunks = new ArrayList<>();

        Iterator iterator = backup_chunks.entrySet().iterator();

        while(iterator.hasNext()){
            ConcurrentHashMap.Entry entry = (ConcurrentHashMap.Entry)iterator.next();
            BackedupChunk chunk = (BackedupChunk) entry.getValue();

            if(chunk.getPeers().contains(key)){
                removeBackupChunk(chunk.getFileId() + "_" + chunk.getChunkNumber(), key, false);
                chunks.add(chunk);
            }
        }

        return chunks;
    }

    public ConcurrentHashMap<String, BackedupChunk> getBacked() {
        return backup_chunks;
    }

    public boolean check_backup(String chunk_id) {
        return backup_chunks.containsKey(chunk_id);
    }


    // --------------------- STORED CHUNKS ----------------------------
    public ConcurrentHashMap<String, StoredChunks> getStoredChunks() {
        return stored_chunks;
    }

    public synchronized void addStoredChunk(BigInteger key, String file_id, int chunk_number, Integer replicationDegree, long chunk_length) {

        if(!stored_chunks.containsKey(file_id)) {
            ArrayList<Integer> chunks_stored = new ArrayList<>();
            chunks_stored.add(chunk_number);
            stored_chunks.put(file_id, new StoredChunks(key, replicationDegree, chunks_stored));

            //update the current space used for storage
            AddOccupiedStorage(chunk_length);
        }
        else if(!checkStoredChunk(file_id, chunk_number)) {
            StoredChunks chunks = stored_chunks.get(file_id);
            chunks.addChunkNumber(chunk_number);
            stored_chunks.replace(file_id, chunks);

            //update the current space used for storage
            AddOccupiedStorage(chunk_length);
        }
    }

    public BigInteger getKeyOfStoredChunk(String file_id) {
        return stored_chunks.get(file_id).getOwner();
    }

    /**
     * checks if the chunk exists in the stored_chunks
     * @param file_id encoded
     * @param chunk_no number of the chunk
     * @return true if exists and false otherwise
     */
    public boolean checkStoredChunk(String file_id, int chunk_no){
        if(stored_chunks.containsKey(file_id)) {
            return stored_chunks.get(file_id).getChunkNumbers().contains(chunk_no);
        }
        else return false;
    }

    public void removeStoredChunk(String file_id, Integer chunk_number) {
        if(stored_chunks.containsKey(file_id)) {
            StoredChunks chunks = stored_chunks.get(file_id);

            if(chunks.getChunkNumbers().size() == 1) {
                stored_chunks.remove(file_id);
            } else {
                chunks.removeChunkNumber(chunk_number);
                stored_chunks.replace(file_id, chunks);
            }
        }
    }

    public boolean removeStoredChunks(String file_id){
        if(!stored_chunks.containsKey(file_id)) {
            return false;
        }
        ArrayList<Integer> chunk_nos = stored_chunks.get(file_id).getChunkNumbers();

        if(chunk_nos.size() == 0) {
            return false;
        }

        stored_chunks.remove(file_id);
        return true;
    }

    public boolean hasStoredChunks(String file_id) {
        if(!stored_chunks.containsKey(file_id)) {
            return false;
        }

        ArrayList<Integer> chunk_nos = stored_chunks.get(file_id).getChunkNumbers();

        if(chunk_nos.size() == 0) {

            //if there aren't more chunks left erase it
            stored_chunks.remove(file_id);
            return false;
        }

        return true;
    }

    // ------------------------------- RESTORE ------------------------------------

    public void addRestoredFile(String file_id, String file_name) {
        restored_files.put(file_id, file_name);
    }

    //---------------------- DELETE ENHANCEMENT ------------------------

    public void deleteFromBackup(String file_id) {
        String file_name = FilesListing.getInstance().getFileName(file_id);
        Integer number_of_chunks = FilesListing.getInstance().getNumberOfChunks(file_name);

        for(int i = 0; i < number_of_chunks; i++) {
            String chunk_id = file_id + "_" + i;

            removeBackupChunk(chunk_id);
        }
    }

    public boolean check_delete(String file_id) {
        return !stored_chunks.contains(file_id);
    }
    // ---------------------------------------------- RECLAIM -----------------------------------

    public void setStorageCapacity(long new_capacity) {
        this.storage_capacity = new_capacity;

        Set<String> keys = stored_chunks.keySet();

        //Obtaining iterator over set entries
        Iterator<String> itr = keys.iterator();
        String file_id;

        //deletes necessary chunk to have that space
        while((this.storage_capacity < occupied_storage) && itr.hasNext()) {

            // Getting Key
            file_id = itr.next();

            ArrayList<Integer> chunks_nos = new ArrayList<>(stored_chunks.get(file_id).getChunkNumbers());

            for(Integer chunk_number : chunks_nos) {
                FileManager.removeChunk(file_id, chunk_number);
                //checks if can stop deleting files
                if(this.storage_capacity >= occupied_storage){
                    break;
                }
            }
        }
    }

    public void removeRestoredFile(String file_id, String file_name){
        restored_files.remove(file_id, file_name);
    }

    // ----------------------------------- GET PATHS ------------------------------------------------------

    public String getRestoredDirectoryPath() {
        return restored_directory_path;
    }

    public String getStoreDirectoryPath() {
        return store_directory_path;
    }
}


