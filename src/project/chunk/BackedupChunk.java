package project.chunk;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;

public class BackedupChunk implements Serializable {
    private final String file_id;
    private final int chunk_number;
    private int replication_degree;
    private ArrayList<BigInteger> peers;

    public BackedupChunk(String file_id, int chunk_number, int replication_degree) {
        this.file_id = file_id;
        this.chunk_number = chunk_number;
        this.replication_degree = replication_degree;
        this.peers = new ArrayList<>();
    }

    public void addPeer(BigInteger peer){
        peers.add(peer);
    }

    public ArrayList<BigInteger> getPeers() {
        return peers;
    }

    public String getFileId() {
        return file_id;
    }

    public int getChunkNumber() {
        return chunk_number;
    }

    public int getReplicationDegree() {
        return replication_degree;
    }

    public void setReplicationDegree(int replication_degree) {
        this.replication_degree = replication_degree;
    }

    public void removePeer(BigInteger key) {
        peers.remove(key);
    }
}
