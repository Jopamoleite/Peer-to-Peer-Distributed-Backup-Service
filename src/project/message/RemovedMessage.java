package project.message;

import project.chunk.Chunk;

import java.math.BigInteger;

public class RemovedMessage extends ProtocolMessage {
    private final Integer chunk_no;

    public RemovedMessage(BigInteger key, String file_id, Integer chunk_no, byte[] chunk) {
        super(Message_Type.REMOVED, key, file_id);
        this.chunk_no = chunk_no;
        this.chunk = chunk;
    }

    public Integer getChunkNo() {
        return chunk_no;
    }

    @Override
    public String getHeader(){
        return super.getHeader() + " " + chunk_no;
    }
}
