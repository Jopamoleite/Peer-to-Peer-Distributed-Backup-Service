package project.message;

import project.Macros;

import java.math.BigInteger;

public class BaseMessage implements java.io.Serializable{
    private final Message_Type message_type;
    private BigInteger sender;
    protected byte[] chunk;

    public BaseMessage(Message_Type message_type, BigInteger sender) {
        this.message_type = message_type;
        this.sender = sender;
        this.chunk = null;
    }

    public BaseMessage(Message_Type message_type) {
        this.message_type = message_type;
        this.chunk = null;
    }

    public String getHeader(){
        return this.message_type + " " + this.sender.toString();
    }

    public byte[] convertMessage(){
        String header = getHeader() + " " + ((char) Macros.CR) + ((char)Macros.LF) + ((char)Macros.CR) + ((char)Macros.LF);

        if(this.chunk == null)
            return header.getBytes();
        else{
            byte[] header_bytes = header.getBytes();
            byte[] message = new byte[this.chunk.length + header_bytes.length];

            System.arraycopy(header_bytes, 0, message, 0, header_bytes.length);
            System.arraycopy(this.chunk, 0, message, header_bytes.length, this.chunk.length);
            return message;
        }
    }

    public Message_Type getMessageType() {
        return message_type;
    }

    public BigInteger getSender() {
        return sender;
    }

    public void setSender(BigInteger sender) {
        this.sender = sender;
    }

    public byte[] getChunk(){ return chunk; }
}
