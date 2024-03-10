package project.protocols;

import project.Macros;
import project.chunk.BackedupChunk;
import project.chunk.Chunk;
import project.chunk.ChunkFactory;
import project.message.BaseMessage;
import project.message.PutChunkMessage;
import project.message.StoredMessage;
import project.peer.ChordNode;
import project.peer.Network;
import project.peer.NodeInfo;
import project.peer.Peer;
import project.store.FileManager;
import project.store.FilesListing;
import project.store.Store;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class BackupProtocol  {

    //------------------------------- peer initiator  ---------------------------------------------------------------
    public static void processPutchunk(int replication_degree, String file_id, ArrayList<Chunk> chunks) {

        //sends putchunks
        for (Chunk chunk : chunks) {
            PutChunkMessage putchunk = new PutChunkMessage(ChordNode.this_node.key, file_id, chunk.chunk_no, replication_degree, chunk.content);

            Store.getInstance().newBackupChunk(file_id, chunk.chunk_no, replication_degree);

            Runnable task = () -> intermediateProcessPutchunk(putchunk, replication_degree);
            Peer.thread_executor.execute(task);
        }
    }

    public static void intermediateProcessPutchunk(PutChunkMessage message, int rep_degree) {
        if(rep_degree > 0){
            Peer.thread_executor.execute(() -> sendPutchunk(message, rep_degree));
            int i = rep_degree - 1;
            Runnable task = () -> intermediateProcessPutchunk(message, i);
            Peer.scheduled_executor.schedule(task, 400, TimeUnit.MILLISECONDS);
        }
    }

    public static void sendPutchunk(PutChunkMessage message, int rep_degree) {
        NodeInfo nodeInfo = getBackupPeer(message.getFileId(), message.getChunkNo(), rep_degree);
        if(nodeInfo != null)
            sendPutchunk(message, rep_degree, 0, nodeInfo);
    }

    public static void sendPutchunk(PutChunkMessage message, int rep_degree, int tries, NodeInfo nodeInfo) {
        if(tries > 10){
            System.out.println("Could not backup chunk " + message.getFileId() + "_" + message.getChunkNo() + " (" + rep_degree + ")");
            return;
        }

        try {
            if(!nodeInfo.key.equals(ChordNode.this_node.key)) {
                StoredMessage stored = (StoredMessage) Network.makeRequest(message, nodeInfo.address, nodeInfo.port);

                if (stored.getStatus().equals(Macros.SUCCESS)) {
                    receiveStored(stored);
                    return;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            //the peer we trying to contact isn't available
        }

        int i = tries + 1;
        NodeInfo new_nodeInfo = ChordNode.findSuccessor(nodeInfo.key.add(new BigInteger("1")));
        Runnable task = () -> sendPutchunk(message, rep_degree, i, new_nodeInfo);
        Peer.scheduled_executor.schedule(task, 400, TimeUnit.MILLISECONDS);
    }

    public static void receiveStored(StoredMessage stored){
        String chunk_id = stored.getFileId() + "_" + stored.getChunkNo();
        BigInteger key = stored.getSender();
        Store.getInstance().addBackupChunks(chunk_id, key);
    }

    // ---------------------- Responses to Peer initiator -----------------------------------------
    public static BaseMessage receivePutchunk(PutChunkMessage putchunk){

        String file_id = putchunk.getFileId();

        //check if this is this peer with a file
        if(Store.getInstance().getFileActualReplicationDegree(file_id + "_" + putchunk.getChunkNo()) != -1) {
            return sendStored(putchunk, Macros.FAIL);
        }

        Boolean x = FileManager.checkConditionsForSTORED(file_id, putchunk.getChunkNo(), putchunk.getChunk().length);
        if(x == null){

            return sendStored(putchunk, Macros.SUCCESS);
        }
        else return sendStored(putchunk, Macros.FAIL);
    }

    private static StoredMessage sendStored(PutChunkMessage putchunk, String status) {
        int chunkNo = putchunk.getChunkNo();
        String fileId = putchunk.getFileId();

        if(status.equals(Macros.SUCCESS))
            FileManager.storeChunk(putchunk.getSender(), fileId, chunkNo, putchunk.getChunk(), putchunk.getReplicationDegree(), false);

        StoredMessage message = new StoredMessage(ChordNode.this_node.key, fileId,  chunkNo, status);
        return message;
    }

    //----------------------------------------------
    public static NodeInfo getBackupPeer(String file_id, int chunk_no, int rep_degree){
        try {
            BigInteger key = ChordNode.generateKey(file_id + ":" + chunk_no + ":" + rep_degree + ":" + Peer.id);
            return ChordNode.findSuccessor(key);
        } catch (NoSuchAlgorithmException e) {
        }
        return null;
    }

    public static void recoverLostChunksReplication(BigInteger key) {
        ArrayList<BackedupChunk> chunks = Store.getInstance().verifyBackupChunks(key);

        for(int i = 0; i < chunks.size(); i++){
            BackedupChunk backup_chunk = chunks.get(i);
            File file = new File(FilesListing.getInstance().getFilePath(backup_chunk.getFileId()));
            Chunk chunk = ChunkFactory.retrieveChunk(file, backup_chunk.getChunkNumber());
            if(chunk == null){
                continue;
            }
            PutChunkMessage putchunk = new PutChunkMessage(ChordNode.this_node.key, backup_chunk.getFileId(),
                    backup_chunk.getChunkNumber(), backup_chunk.getReplicationDegree(), chunk.content);
            Runnable task = ()->sendPutchunk(putchunk, backup_chunk.getReplicationDegree()+1);
            Peer.thread_executor.execute(task);
        }
    }
}
