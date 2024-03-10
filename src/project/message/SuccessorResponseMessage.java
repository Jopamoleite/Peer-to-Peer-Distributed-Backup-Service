package project.message;

import java.math.BigInteger;

public class SuccessorResponseMessage extends BaseMessage{
    private final String status;

    public SuccessorResponseMessage(BigInteger sender, String status) {
        super(Message_Type.SUCCESSOR_RESPONSE, sender);
        this.status = status;
    }

    @Override
    public String getHeader() {
        return super.getHeader() + " " + status;
    }

    public String getStatus() {
        return status;
    }
}
