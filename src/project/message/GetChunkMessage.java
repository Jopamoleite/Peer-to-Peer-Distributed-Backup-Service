package project.message;

import java.math.BigInteger;

public class GetChunkMessage extends ProtocolMessage {
    private final Integer chunk_no;

    public GetChunkMessage(BigInteger key, String file_id, Integer chunk_no) {
        super(Message_Type.GETCHUNK, key, file_id);
        this.chunk_no = chunk_no;
    }

    public Integer getChunkNo() {
        return chunk_no;
    }

    @Override
    public String getHeader(){
        return super.getHeader() + " " + chunk_no;
    }
}