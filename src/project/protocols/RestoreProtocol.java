package project.protocols;

import project.Macros;
import project.chunk.Chunk;
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

public class RestoreProtocol {
    // --------- peer initiator
    public static void processGetChunk(String file_id, int number_of_chunks){
        for (int i = 0; i < number_of_chunks; i++) {
            int chunk_no = i;
            Runnable task = () -> sendGetChunk(file_id, chunk_no);
            Peer.thread_executor.execute(task);
        }
    }

    public static void sendGetChunk(String file_id, int chunk_no){
        ArrayList<BigInteger> peers = Store.getInstance().getBackupChunksPeers(file_id + "_" + chunk_no);

        for(int i=0; i < peers.size(); i++){
            NodeInfo nodeInfo = ChordNode.findSuccessor(peers.get(i));
            if(nodeInfo.key.equals(peers.get(i))){
                GetChunkMessage message = new GetChunkMessage(nodeInfo.key, file_id, chunk_no);
                try {
                    ChunkMessage chunk = (ChunkMessage) Network.makeRequest(message, nodeInfo.address, nodeInfo.port);
                    receiveChunk(chunk);
                    return;
                } catch (IOException | ClassNotFoundException e) {
                    //the peer we trying to contact isn't available
                }
            }
        }
        System.out.println("Failed to retrieve chunk " + chunk_no + " of backed up file " + file_id);
    }

    public static void receiveChunk(ChunkMessage chunkMessage){
        String chunk_id = chunkMessage.getFileId() + "_" + chunkMessage.getChunkNo();

        if (Store.getInstance().getFileActualReplicationDegree(chunk_id) != -1) {
            FileManager.writeChunkToRestoredFile(FilesListing.getInstance().getFileName(chunkMessage.getFileId()),
                    chunkMessage.getChunk(), chunkMessage.getChunkNo());
        }
    }


    //---------------- peer not initiator

    /**
     * a peer that has a copy of the specified chunk shall send it in the body of a CHUNK message via the MDR channel
     * @param  getChunkMessage message received
     */
    public static ChunkMessage receiveGetChunk(GetChunkMessage getChunkMessage ){
        String file_id = getChunkMessage.getFileId();
        Integer chunk_number = getChunkMessage.getChunkNo();

        Chunk chunk = FileManager.retrieveChunk(file_id, chunk_number);

        if (chunk == null)
            return sendChunk(Macros.FAIL, file_id, chunk_number, null);

        return sendChunk(Macros.SUCCESS, file_id, chunk_number, chunk.content);
    }

    public static ChunkMessage sendChunk(String status, String file_id, Integer chunk_no, byte[] chunk_data){
        return new ChunkMessage(ChordNode.this_node.key, status, file_id, chunk_no, chunk_data);
    }
}
