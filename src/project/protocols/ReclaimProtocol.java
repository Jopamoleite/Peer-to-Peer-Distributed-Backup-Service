package project.protocols;

import project.chunk.Chunk;
import project.message.*;
import project.peer.ChordNode;
import project.peer.Network;
import project.peer.NodeInfo;
import project.peer.Peer;
import project.store.Store;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

public class ReclaimProtocol {

    // --------------------- peer initiator ------------------------------------------
    public static void sendRemoved(String file_id, int chunk_number, Chunk chunk, BigInteger owner) {
        RemovedMessage removedMessage = new RemovedMessage(ChordNode.this_node.key, file_id, chunk_number, chunk.content);

        Runnable task = () -> processRemovedMessage(removedMessage, owner, 0);
        Peer.thread_executor.execute(task);
    }

    public static void processRemovedMessage(RemovedMessage message, BigInteger owner, int tries) {
        if(tries > 10){
            System.out.println("Could not contact owner of chunk.");
            return;
        }

        NodeInfo nodeInfo = ChordNode.findSuccessor(owner);
        if(owner.equals(nodeInfo.key)) {
            try {
                Network.makeRequest(message, nodeInfo.address, nodeInfo.port);
                return;
            } catch (IOException | ClassNotFoundException e) {
            }
        }

        int i = tries + 1;
        Runnable task = () -> processRemovedMessage(message, owner, i);
        Peer.scheduled_executor.schedule(task, 400, TimeUnit.MILLISECONDS);
    }

    // -------------- receiver peer

    public static BaseMessage receiveRemoved(RemovedMessage removed) {
        String file_id = removed.getFileId();
        int chunk_no = removed.getChunkNo();
        String chunk_id = file_id + "_" + chunk_no;

        Store.getInstance().removeBackupChunk(chunk_id, removed.getSender(), false);

        int rep_degree = Store.getInstance().getFileReplicationDegree(chunk_id);
        int actual_rep_degree = Store.getInstance().getFileActualReplicationDegree(chunk_id);

        if(actual_rep_degree < rep_degree ) {
            PutChunkMessage putchunk = new PutChunkMessage(ChordNode.this_node.key, file_id, chunk_no, rep_degree, removed.getChunk());

            Runnable task = () -> BackupProtocol.sendPutchunk(putchunk, actual_rep_degree);
            Peer.thread_executor.execute(task);
        }

        return new MockMessage(ChordNode.this_node.key);
    }
}

