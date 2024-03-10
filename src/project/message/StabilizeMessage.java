package project.message;

import java.math.BigInteger;

public class StabilizeMessage extends BaseMessage{
    public StabilizeMessage(BigInteger sender) {
        super(Message_Type.STABILIZE, sender);
    }
}
