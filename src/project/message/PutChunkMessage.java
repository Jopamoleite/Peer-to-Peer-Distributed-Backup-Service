package project.message;

import java.math.BigInteger;

public class PutChunkMessage extends ProtocolMessage {
    private final int chunk_no;
    private final int replication_degree;

    public PutChunkMessage(BigInteger key, String file_id, int chunk_no, int replication_degree, byte[] chunk) {
        super(Message_Type.PUTCHUNK, key, file_id);
        this.chunk_no = chunk_no;
        this.replication_degree = replication_degree;
        this.chunk = chunk;
    }

    public int getChunkNo(){
        return chunk_no;
    }
    
    public int getReplicationDegree(){
        return replication_degree;
    }

    @Override
    public String getHeader(){
        return super.getHeader() + " " + chunk_no + " " + replication_degree;
    }
}
