package project.message;

import project.protocols.*;

public class MessageHandler {
    public static BaseMessage handleMessage(BaseMessage message){
            switch (message.getMessageType()) {
                case PUTCHUNK:
                    return BackupProtocol.receivePutchunk((PutChunkMessage) message);
                case GETCHUNK:
                    return RestoreProtocol.receiveGetChunk((GetChunkMessage) message);
                case DELETE:
                    return DeleteProtocol.receiveDelete((DeleteMessage) message);
                case REMOVED:
                    return ReclaimProtocol.receiveRemoved((RemovedMessage) message);
                case CONNECTION_REQUEST:
                    return ConnectionProtocol.receiveRequest((ConnectionRequestMessage) message);
                case REQUEST_PREDECESSOR:
                    return ConnectionProtocol.receiveRequestPredecessor((RequestPredecessorMessage) message);
                case FIND_PREDECESSOR:
                    return ConnectionProtocol.receiveFindPredecessor((FindNodeMessage) message);
                case FIND_SUCCESSOR:
                    return ConnectionProtocol.receiveFindSuccessor((FindNodeMessage) message);
                case NOTIFY_SUCCESSOR:
                    return ConnectionProtocol.receiveNotifySuccessor((NotifySuccessorMessage) message);
                case STABILIZE:
                    return ConnectionProtocol.receivedStabilize();
                case DISCONNECT:
                    return ConnectionProtocol.receivedDisconnectMessage((DisconnectMessage) message);
                case NOTIFY_STORAGE:
                    return StorageRestoreProtocol.receiveNotifyStorage((NotifyStorageMessage) message);
                case CHUNK:
                case STORED:
                case DELETE_RECEIVED:
                case CONNECTION_RESPONSE:
                case PREDECESSOR_RESPONSE:
                case SUCCESSOR_RESPONSE:
                case PREDECESSOR:
                case SUCCESSOR:
                case STABILIZE_RESPONSE:
                case STORAGE_RESPONSE:
                case MOCK:
                    return message;
                default:
                    break;
            }
        return null;
    }
}
