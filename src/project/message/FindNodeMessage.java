package project.message;

import java.math.BigInteger;

public class FindNodeMessage extends BaseMessage{
    private final BigInteger key;

    public FindNodeMessage(Message_Type type, BigInteger sender, BigInteger key) {
        super(type, sender);
        this.key = key;
    }

    @Override
    public String getHeader() {
        return super.getHeader() + " " + key.toString();
    }

    public BigInteger getKey() {
        return key;
    }
}
