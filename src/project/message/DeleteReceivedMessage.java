package project.message;

import java.math.BigInteger;

public class DeleteReceivedMessage extends ProtocolMessage {
        public DeleteReceivedMessage(BigInteger sender, String file_id) {
        super(Message_Type.DELETE_RECEIVED, sender, file_id);
    }
}
