package project.message;

import project.Macros;

import java.math.BigInteger;

public class StoredMessage extends ProtocolMessage {
    private final int chunk_no;
    private final String status;

    public StoredMessage(BigInteger key, String file_id, int chunk_no, String status) {
        super(Message_Type.STORED, key, file_id);
        this.chunk_no = chunk_no;
        this.status = status;
    }

    @Override
    public String getHeader(){
        return super.getHeader() + " " + chunk_no + " " + status;
    }

    public int getChunkNo(){
        return chunk_no;
    }

    public String getStatus(){return status;}
}
