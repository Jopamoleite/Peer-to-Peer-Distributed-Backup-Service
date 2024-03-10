package project.message;

import java.math.BigInteger;

public class MockMessage extends BaseMessage {
    public MockMessage(BigInteger sender) {
        super(Message_Type.MOCK, sender);
    }
}
