package project.message;

import java.math.BigInteger;

public class NotifySuccessorMessage  extends BaseMessage{
    private final String address;
    private final int port;

    public NotifySuccessorMessage(BigInteger sender, String address, int port) {
        super(Message_Type.NOTIFY_SUCCESSOR, sender);
        this.address = address;
        this.port = port;
    }

    @Override
    public String getHeader() {
        return super.getHeader() + " " + address + " " + port;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }
}
