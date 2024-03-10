package project.message;

import java.math.BigInteger;

public class DisconnectMessage extends BaseMessage{
    private final BigInteger key;
    private final String address;
    private final int port;

    public DisconnectMessage(BigInteger sender, BigInteger key, String address, int port) {
        super(Message_Type.DISCONNECT, sender);
        this.key = key;
        this.address = address;
        this.port = port;
    }

    @Override
    public String getHeader() {
        return super.getHeader() + " " + key.toString() + " " + address + " " + port;
    }

    public BigInteger getKey(){
        return key;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }
}
