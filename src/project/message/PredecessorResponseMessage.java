package project.message;

import java.math.BigInteger;

public class PredecessorResponseMessage extends BaseMessage{

    public PredecessorResponseMessage(BigInteger sender, byte[] finger_table) {
        super(Message_Type.PREDECESSOR_RESPONSE, sender);
        this.chunk = finger_table;
    }
}
