package project.protocols;

import project.message.*;
import project.peer.ChordNode;
import project.peer.Network;
import project.peer.NodeInfo;
import project.peer.Peer;
import project.store.FileManager;
import project.store.FilesListing;
import project.store.Store;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class DeleteProtocol {
    public static void processDelete(String file_id) {
        String file_name = FilesListing.getInstance().getFileName(file_id);
        int number_of_chunks = FilesListing.getInstance().getNumberOfChunks(file_name);

        DeleteMessage delete = new DeleteMessage(ChordNode.this_node.key, file_id);

        ArrayList<BigInteger> keys = new ArrayList<>();
        for(int i = 0; i < number_of_chunks; i++) {
            String chunk_id = file_id + "_" + i;
            keys.addAll(Store.getInstance().getBackupChunksPeers(chunk_id));
        }
        Set<BigInteger> key_set = new LinkedHashSet(keys);

        for(BigInteger key : key_set){
            Runnable task = () -> sendDelete(delete, key, number_of_chunks, 0);
            Peer.thread_executor.execute(task);
        }

        // Remove entry with the file_name and correspond file_id from allFiles
        FilesListing.getInstance().deleteFileRecords(file_name);
    }

    public static void sendDelete(DeleteMessage delete, BigInteger key, int number_of_chunks, int tries) {
        if(tries >= 10){
            System.out.println("Couldn't delete all chunks of the file " + delete.getFileId());
            return;
        }

        NodeInfo nodeInfo = ChordNode.findSuccessor(key);
        if(nodeInfo.key.equals(key)) {
            try {
                DeleteReceivedMessage response = (DeleteReceivedMessage) Network.makeRequest(delete, nodeInfo.address, nodeInfo.port);
                receiveDeleteReceived(response, number_of_chunks);
                return;
            } catch (IOException | ClassNotFoundException e) {
                //the peer we trying to contact isn't available
            }
        }

        int n = tries + 1;
        Runnable task = ()->sendDelete(delete, key, number_of_chunks, n);
        Peer.scheduled_executor.schedule(task, (int)Math.pow(3, n), TimeUnit.SECONDS);
    }

    public static DeleteReceivedMessage receiveDelete(DeleteMessage deleteMessage){
        String file_id = deleteMessage.getFileId();

        //delete all files and records in stored
        FileManager.deleteFilesFolders(file_id);

        if (Store.getInstance().removeStoredChunks(file_id)) {
            System.out.println("Remove file " + deleteMessage.getFileId() + " records");
        }

        return new DeleteReceivedMessage(ChordNode.this_node.key, file_id);
    }

    public static void receiveDeleteReceived(DeleteReceivedMessage message, int number_of_chunks) {
        BigInteger key = message.getSender();
        String file_id = message.getFileId();

        for(int i = 0; i < number_of_chunks; i++) {
            String chunk_id = file_id + "_" + i;
            Store.getInstance().removeBackupChunk(chunk_id, key);
        }

        System.out.println("Deletion of all chunks of file " + file_id + " on peer " + key + " successful");
    }
}


