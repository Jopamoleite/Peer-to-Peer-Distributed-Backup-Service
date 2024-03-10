package project.message;

import java.math.BigInteger;

public class NodeMessage extends BaseMessage {
    private final String address;
    private final int port;
    private final BigInteger key;

    public NodeMessage(Message_Type type, BigInteger sender, BigInteger key, String address, int port) {
        super(type, sender);
        this.key = key;
        this.address = address;
        this.port = port;
    }

    @Override
    public String getHeader() {
        return super.getHeader() + " " + key.toString() + " " + address + " " + port;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public BigInteger getKey() {
        return key;
    }
}
