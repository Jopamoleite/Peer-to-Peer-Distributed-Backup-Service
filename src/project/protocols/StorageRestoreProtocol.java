package project.protocols;

import project.chunk.BackedupChunk;
import project.chunk.Chunk;
import project.chunk.StoredChunks;
import project.message.BaseMessage;
import project.message.NotifyStorageMessage;
import project.message.PutChunkMessage;
import project.message.StorageResponseMessage;
import project.peer.ChordNode;
import project.peer.Network;
import project.peer.NodeInfo;
import project.peer.Peer;
import project.store.FileManager;
import project.store.Store;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class StorageRestoreProtocol {

    public static void processNotifyStorage() {
        //first send to the peer initiators a notification of the files saved of him
        ConcurrentHashMap<String, StoredChunks> stored_chunks = Store.getInstance().getStoredChunks();

        for (String key: stored_chunks.keySet()) {
            StoredChunks storedChunks = stored_chunks.get(key);

            NotifyStorageMessage notifyStorage = new NotifyStorageMessage(ChordNode.this_node.key, storedChunks.getChunkNumbers(), key, false);

            Runnable task = ()->sendNotifyStorage(notifyStorage, storedChunks.getOwner(), 0);
            Peer.thread_executor.execute(task);
        }

        //second checks his own files and replies the ones that were deleted when he was down
        ConcurrentHashMap<String, BackedupChunk> backedUpChunk = Store.getInstance().getBacked();

        for (String key: backedUpChunk.keySet()) {
            BackedupChunk backedUpChunks = backedUpChunk.get(key);

            ArrayList<Integer> chunk_number = new ArrayList<>();
            chunk_number.add(backedUpChunks.getChunkNumber());
            NotifyStorageMessage notifyStorage = new NotifyStorageMessage(ChordNode.this_node.key, chunk_number, backedUpChunks.getFileId(), true);

            ArrayList<BigInteger> peers = backedUpChunks.getPeers();

            for(BigInteger peerKey: peers) {
                Runnable task = ()-> sendNotifyStorage(notifyStorage, peerKey, 0);
                Peer.thread_executor.execute(task);
            }
        }

    }

    public static void sendNotifyStorage(NotifyStorageMessage notifyStorage, BigInteger owner, int tries) {
        if(tries >= 10){
            System.out.println("Couldn't notify storage of chunks " + notifyStorage.getFileId() + " of the owner " + owner);
            return;
        }

        NodeInfo nodeInfo = ChordNode.findSuccessor(owner);
        if(nodeInfo.key.equals(owner)) {
            try {
                StorageResponseMessage response = (StorageResponseMessage) Network.makeRequest(notifyStorage, nodeInfo.address, nodeInfo.port);
                receiveStorageResponse(response);
                return;
            } catch (IOException | ClassNotFoundException e) {
                //the peer we trying to contact isn't available
            }
        }

        int n = tries + 1;
        Runnable task = ()->sendNotifyStorage(notifyStorage, owner, n);
        Peer.scheduled_executor.schedule(task, (int)Math.pow(2, n), TimeUnit.SECONDS);

    }

    private static void receiveStorageResponse(StorageResponseMessage response) {

        String file_id = response.getFile_id();
        ArrayList<Integer> not_found_chunk_numbers = response.get_not_foundChunk_number();

        if(not_found_chunk_numbers != null) {
            if (response.isStore()) {
                //is possible only some chunks were deleted so is necessary to do this operation chunk be chunk
                for (Integer chunk_number : not_found_chunk_numbers) {
                    String chunk_id = file_id + "_" + chunk_number;

                    //replication degree isn't the desire one
                    Store.getInstance().removeStoredChunk(file_id, chunk_number);


                    int rep_degree = Store.getInstance().getFileActualReplicationDegree(chunk_id);
                    int actual_rep_degree = Store.getInstance().getFileActualReplicationDegree(chunk_id);

                    Chunk chunk = FileManager.retrieveChunk(file_id, chunk_number);
                    if (chunk != null) {
                        PutChunkMessage putchunk = new PutChunkMessage(ChordNode.this_node.key, file_id, chunk_number, rep_degree, chunk.content);

                        //done chunk by chunk
                        Runnable task = () -> BackupProtocol.sendPutchunk(putchunk, rep_degree + 1);
                        Peer.thread_executor.execute(task);
                    }
                }

            } else {
                //is not possible to only have chunks of the file deleted, so delete it all
                System.out.println("File " + file_id +" was deleted during fault. Deleting");

                //file was deleted so deleting all files and records in stored
                FileManager.deleteFilesFolders(file_id);

            }
        }
        //otherwise everything is ok

    }

    public static BaseMessage receiveNotifyStorage(NotifyStorageMessage notify) {

        ArrayList<Integer> chunk_numbers = notify.getChunk_numbers();
        String file_id = notify.getFileId();

        //checking if it is still store
        if(notify.isCheckStorage()) {
            ArrayList<Integer> found = new ArrayList<>();
            ArrayList<Integer> not_found = new ArrayList<>();

            for(Integer chunk_no : chunk_numbers) {
                if(Store.getInstance().checkStoredChunk(file_id, chunk_no) ) {
                    found.add(chunk_no);
                } else {
                    not_found.add(chunk_no);
                }
            }
            return new StorageResponseMessage(ChordNode.this_node.key, found, not_found, file_id, notify.isCheckStorage());
        } else {
            String chunk_id = file_id + "_" + 0;
            //our the file is store our it is not store, can not have only some chunks
            if(!Store.getInstance().check_backup(chunk_id)) {
                return new StorageResponseMessage(ChordNode.this_node.key, chunk_numbers, file_id, notify.isCheckStorage(), false);
            } else {
                //add the peer to the list of Peers containing the file
                for (Integer chunk_no : chunk_numbers) {
                    Store.getInstance().addBackupChunks(file_id + "_" + chunk_no, notify.getSender());
                }
                return new StorageResponseMessage(ChordNode.this_node.key, chunk_numbers, file_id, notify.isCheckStorage(), true);
            }
        }

    }
}
