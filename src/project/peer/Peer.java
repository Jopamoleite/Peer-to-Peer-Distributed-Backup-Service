package project.peer;

import java.io.*;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.*;
import project.InvalidFileException;
import project.Macros;

import project.chunk.Chunk;
import project.chunk.ChunkFactory;
import project.chunk.StoredChunks;
import project.message.InvalidMessageException;
import project.protocols.BackupProtocol;
import project.protocols.DeleteProtocol;
import project.protocols.RestoreProtocol;
import project.protocols.StorageRestoreProtocol;
import project.store.FileInfo;
import project.store.FileManager;
import project.store.FilesListing;
import project.Pair;
import project.store.Store;

import static project.store.FileManager.deleteFileFolder;

public class Peer implements RemoteInterface {
    private static final int RegistryPort = 1099;
    public static int id;

    private static String service_access_point;

    public static ScheduledThreadPoolExecutor scheduled_executor = new ScheduledThreadPoolExecutor(25);
    public static ExecutorService thread_executor = Executors.newCachedThreadPool();

    public static ChordNode node;

    public Peer(int port) throws IOException, NoSuchAlgorithmException {
        node = new ChordNode(port);
    }

    public Peer(int port, String neighbour_address, int neighbour_port) throws IOException, NoSuchAlgorithmException {
        node = new ChordNode(port, neighbour_address, neighbour_port);
    }

    private static void usage(){
        System.out.println("Usage: java [package]Peer <peer_id> <service_access_point> <port> [<neighbour_address> <neighbour_port>]");
    }

    //class methods
    public static void main(String[] args){
        if(args.length < 3){
            usage();
            return;
        }

        try{
            id = Integer.parseInt(args[0]);

            service_access_point = args[1];

            int port = Integer.parseInt(args[2]);

            if(!Macros.checkPort(port)){
                return;
            }

            setSslProperties();

            Peer object_peer;

            try{
                if(args.length == 3){
                    object_peer = new Peer(port);
                }
                else if (args.length == 5) {
                    object_peer = new Peer(port, args[3], Integer.parseInt(args[4]));
                }
                else{
                    usage();
                    return;
                }
            }
            catch( IOException e) {
                System.out.println("Server - Failed to create SSLServerSocket");
                System.out.println(e.getMessage());
                return;
            }

            RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(object_peer, 0);
            Registry registry;
            try {
                LocateRegistry.createRegistry(RegistryPort);
            } catch (RemoteException ignored){
            }

            registry = LocateRegistry.getRegistry(RegistryPort);

            registry.rebind(service_access_point, stub);

            if(loadStorage()){
                System.out.println("Storage state loaded from file!");
                Peer.scheduled_executor.schedule(()-> StorageRestoreProtocol.processNotifyStorage(), 10, TimeUnit.SECONDS);
            } else {
                Store.getInstance().initializeStore();
                FilesListing.getInstance();
            }

            scheduled_executor.scheduleAtFixedRate(Peer::saveStorage, 15, 15, TimeUnit.SECONDS);
            Runtime.getRuntime().addShutdownHook(new Thread(Peer::saveStorage));

        } catch (Exception e) {
            System.err.println("Peer exception: " + e.toString());
        }
    }

    public static void setSslProperties(){
        System.setProperty("javax.net.ssl.keyStore", "server.keys");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");
        System.setProperty("javax.net.ssl.trustStore", "truststore");
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");
        System.setProperty("java.net.preferIPv4Stack", "true");
    }

    public int backup(String file_path, int replication_degree) throws InvalidMessageException, InvalidFileException {

        if(replication_degree <= 0 || replication_degree > 9)
            throw new InvalidMessageException("Replication degree is invalid");

        System.out.println("Backup file: "+ file_path);

        File file = new File(file_path);

        if(file.length() >= Macros.MAX_FILE_SIZE) {
            throw new InvalidFileException("File is larger than accepted");
        }

        String file_id = FileManager.createFileId(file);
        int number_of_chunks = (int) Math.ceil((float) file.length() / Macros.CHUNK_MAX_SIZE );
        if(!FilesListing.getInstance().addFile(file.getName(), file_id, number_of_chunks, file_path)) {
            System.out.println("File was backup in a previous session and hasn't been change since them");
            return 0;
        }

        BackupProtocol.processPutchunk(replication_degree, file_id, ChunkFactory.produceChunks(file, replication_degree));

        return 0;
    }

    /**
     *
     * @param file_path
     * The client shall specify the file to restore by its pathname.
     */
    @Override
    public int restore(String file_path) throws InvalidFileException {

        final String file_name = new File(file_path).getName();

        String file_id;
        try{
            file_id = FilesListing.getInstance().getFileId(file_name);
        }
        catch(Exception e){
            throw new InvalidFileException("File name not found");
        }

        FileManager.createEmptyFileForRestoring( file_name );

        int number_of_chunks = FilesListing.getInstance().getNumberOfChunks(file_name);

        RestoreProtocol.processGetChunk(file_id, number_of_chunks);

        Store.getInstance().addRestoredFile(file_id, file_name);

        return 0;
    }

    /**
     * The client shall specify the file to delete by its pathname.
     * @param file_path of the file that is going to be deleted
     */
    @Override
    public int delete(String file_path) throws InvalidFileException {
        final String file_name = new File(file_path).getName();

        //gets the file_id from the entry with key file_name form allFiles
        String file_id;
        try{
            file_id = FilesListing.getInstance().getFileId(file_name);
        }
        catch(Exception e){
            throw new InvalidFileException("File name not found");
        }

        //removes restored files from storage
        deleteFileFolder( Store.getInstance().getRestoredDirectoryPath() + file_name, false );
        Store.getInstance().removeRestoredFile(file_id, file_name);

        //sends message DELETE to all peers
        DeleteProtocol.processDelete(file_id);

        return 0;
    }


    /**
     *
     * @param max_disk_space
     * The client shall specify the maximum disk space in KBytes (1KByte = 1000 bytes) that can be used for storing chunks.
     * It must be possible to specify a value of 0, thus reclaiming all disk space previously allocated to the service.
     */
    @Override
    public int reclaim(int max_disk_space) {
        if(max_disk_space < 0) {
            System.err.println("Invalid maximum disk space");
            System.exit(-1);
        }

        long max_disk_space_aux = 1000*(long)max_disk_space;

        Runnable task = ()-> Store.getInstance().setStorageCapacity(max_disk_space_aux);
        Peer.thread_executor.execute(task);

        return 0;
    }

    @Override
    public String state() {
        String state = "\n------- THISISPEER " + Peer.id + " -------\n";
        state += "------- ( " + Peer.service_access_point + " ) -------\n";
        state += "------------------------------------\n\n";

        state += retrieveBackupState() + "\n";

        state += retrieveStoredChunksState() + "\n";

        state += retrieveStorageState();

        return state;
    }

    private String retrieveBackupState() {
        String state = "|--------- BACKUP --------|\n";
        ConcurrentHashMap<String, FileInfo> files = FilesListing.getInstance().getFiles();

        Iterator it = files.entrySet().iterator();

        while(it.hasNext()){
            ConcurrentHashMap.Entry file = (ConcurrentHashMap.Entry)it.next();
            String file_name = (String) file.getKey();
            FileInfo info = (FileInfo) file.getValue();
            if(info.getFileId()!=null){
                int replication_degree = Store.getInstance().getFileReplicationDegree(info.getFileId() + "_0");

                state = state + "> path: " + file_name + "\n"
                        + "   id: " + info.getFileId() + "\n"
                        + "   replication degree: " + replication_degree + "\n"
                        + "   > chunks:\n";

                for (int i = 0; i < info.getNumberOfChunks(); i++) {
                    state = state + "      id: " + i + "\n"
                            + "         perceived replication degree: " + Store.getInstance().getFileActualReplicationDegree(info.getFileId() + "_" + i) + "\n";
                }
            }
        }

        return state;
    }

    private String retrieveStoredChunksState() {
        String state = "|----- STORED CHUNKS -----|\n";

        ConcurrentHashMap<String, StoredChunks> stored_chunks = Store.getInstance().getStoredChunks();
        Iterator it = stored_chunks.entrySet().iterator();

        while(it.hasNext()) {
            ConcurrentHashMap.Entry chunks = (ConcurrentHashMap.Entry) it.next();
            String file_id = (String) chunks.getKey();
            Pair<Integer,ArrayList<Integer>> pair = (Pair<Integer,ArrayList<Integer>>) chunks.getValue();// Pair( replication degree , chunks ids )

            state = state + "> file_id: " + file_id + "\n";

            for(Integer chunk_no : pair.second){
                state = state + "   id: " + chunk_no + "\n"
                        + "      size: " + FileManager.retrieveChunkSize(file_id, chunk_no) + "\n"
                        + "      for peer with key: " + Store.getInstance().getKeyOfStoredChunk((file_id + "_" + chunk_no)) + "\n";
            }
        }

        return state;
    }

    private String retrieveStorageState() {
        String state = "|----- STORAGE STATE -----|\n";

        state = state + "   Capacity: " + Store.getInstance().getStorageCapacity() + "\n"
                + "   Occupied: " + Store.getInstance().getOccupiedStorage() + "\n";

        return state + "---------------------------";
    }

    private static void saveStorage() {
        try {
            String storage_file = Peer.id + "_directory/store.ser";

            File file = new File(storage_file);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            FileOutputStream file_output = new FileOutputStream(storage_file);
            ObjectOutputStream output = new ObjectOutputStream(file_output);
            output.writeObject(Store.getInstance());
            output.close();
            file_output.close();

            String file_listings_file = Peer.id + "_directory/file_listings.ser";

            File file2 = new File(file_listings_file);
            if (!file2.exists()) {
                file2.getParentFile().mkdirs();
                file2.createNewFile();
            }

            FileOutputStream file_output2 = new FileOutputStream(file_listings_file);
            ObjectOutputStream output2 = new ObjectOutputStream(file_output2);
            output2.writeObject(FilesListing.getInstance());
            output2.close();
            file_output2.close();

        } catch (IOException i) {
        }
    }

    private static boolean loadStorage() {
        try {
            String storage_file = Peer.id + "_directory/store.ser";

            File file = new File(storage_file);
            if (!file.exists()) {
                return false;
            }

            String file_listings_file = Peer.id + "_directory/file_listings.ser";

            File file2 = new File(file_listings_file);
            if (!file2.exists()) {
                return false;
            }

            FileInputStream storage_file_input = new FileInputStream(storage_file);
            ObjectInputStream storage_input = new ObjectInputStream(storage_file_input);
            Store.setInstance((Store) storage_input.readObject());
            storage_input.close();
            storage_file_input.close();


            FileInputStream listing_file_input = new FileInputStream(file_listings_file);
            ObjectInputStream listing_input = new ObjectInputStream(listing_file_input);
            FilesListing.setInstance((FilesListing) listing_input.readObject());
            listing_input.close();
            listing_file_input.close();

        } catch (Exception e) {
            return false;
        }
        return true;
    }
}

