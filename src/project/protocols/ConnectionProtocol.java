package project.protocols;

import project.Macros;
import project.message.*;
import project.peer.ChordNode;
import project.peer.Network;
import project.peer.NodeInfo;
import project.peer.Peer;

import java.io.IOException;
import java.math.BigInteger;

public class ConnectionProtocol {
    public static void connectToNetwork(String neighbour_address, int neighbour_port) {
        ConnectionRequestMessage request = new ConnectionRequestMessage(ChordNode.this_node.key, ChordNode.this_node.address, ChordNode.this_node.port);

        try {
            ConnectionResponseMessage response = (ConnectionResponseMessage) Network.makeRequest(request, neighbour_address, neighbour_port);
            ChordNode.setPredecessor(response.getPredecessor(), response.getAddress(), response.getPort());
        } catch (IOException | ClassNotFoundException e) {
        }

        try {
            RequestPredecessorMessage contact_predecessor = new RequestPredecessorMessage(ChordNode.this_node.key, ChordNode.this_node.address, ChordNode.this_node.port);

            PredecessorResponseMessage predecessor_response = (PredecessorResponseMessage) Network.makeRequest(contact_predecessor, ChordNode.predecessor.address, ChordNode.predecessor.port);
            if(predecessor_response.getChunk().length != 0){
                ChordNode.setSuccessor(new String(predecessor_response.getChunk()).trim());
                notifySuccessor();
            }
            NodeInfo successor2 = ChordNode.findSuccessor(ChordNode.successorHelper(ChordNode.getSuccessorNode().key));
            NodeInfo successor3 = ChordNode.findSuccessor(ChordNode.successorHelper(successor2.key));
            ChordNode.successors.add(0, ChordNode.getSuccessorNode());
            ChordNode.successors.add(1, successor2);
            ChordNode.successors.add(2, successor3);
        } catch (IOException | ClassNotFoundException e) {
        }
    }

    public static BaseMessage receiveRequest(ConnectionRequestMessage message) {
        NodeInfo predecessor = ChordNode.findPredecessor(message.getSender());
        ChordNode.setPredecessor(message.getSender(), message.getAddress(), message.getPort());
        return new ConnectionResponseMessage(ChordNode.this_node.key, predecessor.key, predecessor.address, predecessor.port);
    }

    public static BaseMessage receiveRequestPredecessor(RequestPredecessorMessage message) {
        NodeInfo successor = new NodeInfo(message.getSender(), message.getAddress(), message.getPort());
        PredecessorResponseMessage response = new PredecessorResponseMessage(ChordNode.this_node.key, ChordNode.getSuccessor());

        ChordNode.successors.add(2, ChordNode.successors.get(1));
        ChordNode.successors.add(1, ChordNode.getSuccessorNode());
        ChordNode.successors.add(0, successor);
        ChordNode.addSuccessor(successor);

        return response;
    }

    public static boolean notifySuccessor() {
        try {
            NodeInfo successor = ChordNode.getSuccessorNode();
            NotifySuccessorMessage contact_successor = new NotifySuccessorMessage(ChordNode.this_node.key, ChordNode.this_node.address, ChordNode.this_node.port);
            Network.makeRequest(contact_successor, successor.address, successor.port);
            return true;
        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }

    public static BaseMessage receiveNotifySuccessor(NotifySuccessorMessage message) {
        String status = ChordNode.setPredecessor(message.getSender(), message.getAddress(), message.getPort());
        SuccessorResponseMessage response = new SuccessorResponseMessage(ChordNode.this_node.key, status);

        return response;
    }

    public static NodeInfo findSuccessor(BigInteger key, NodeInfo node) {

        while(!node.key.equals(ChordNode.this_node.key)){
            try {
                NodeMessage successor = (NodeMessage) Network.makeRequest(new FindNodeMessage(Message_Type.FIND_SUCCESSOR, ChordNode.this_node.key, key), node.address, node.port);
                return new NodeInfo(successor.getKey(), successor.getAddress(), successor.getPort());
            } catch (IOException | ClassNotFoundException e) {
                node = ChordNode.findPreviousFinger(node.key);
            }
        }
        return ChordNode.this_node;
    }

    public static NodeInfo findPredecessor(BigInteger key, NodeInfo node) {
        try {
            NodeMessage predecessor = (NodeMessage) Network.makeRequest(new FindNodeMessage(Message_Type.FIND_PREDECESSOR, ChordNode.this_node.key, key), node.address, node.port);

            return new NodeInfo(predecessor.getKey(), predecessor.getAddress(), predecessor.getPort());
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    public static BaseMessage receiveFindPredecessor(FindNodeMessage message) {
        NodeInfo predecessor = ChordNode.findPredecessor(message.getKey());
        return new NodeMessage(Message_Type.PREDECESSOR, ChordNode.this_node.key, predecessor.key, predecessor.address, predecessor.port);
    }

    public static BaseMessage receiveFindSuccessor(FindNodeMessage message) {
        NodeInfo successor = ChordNode.findSuccessor(message.getKey());
        return new NodeMessage(Message_Type.SUCCESSOR, ChordNode.this_node.key, successor.key, successor.address, successor.port);
    }


    public static StabilizeResponseMessage stabilize(NodeInfo node, boolean final_successor) {
        try {
            return (StabilizeResponseMessage) Network.makeRequest(new StabilizeMessage(ChordNode.this_node.key), node.address, node.port);
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Successor is down");
            if(final_successor)
                ChordNode.fingerTableRecovery(node.key);
            Peer.thread_executor.execute(()->BackupProtocol.recoverLostChunksReplication(node.key));
            Peer.thread_executor.execute(()->sendDisconnectMessage(new DisconnectMessage(ChordNode.this_node.key, node.key, node.address, node.port)));

            return null;
        }
    }

    public static boolean checkPredecessor() {
        if(ChordNode.predecessor != null){
            try {
                Network.makeRequest(new StabilizeMessage(ChordNode.this_node.key), ChordNode.predecessor.address, ChordNode.predecessor.port);
                return true;
            } catch (IOException | ClassNotFoundException e) {
                //the peer we trying to contact isn't available
            }
        }
        return false;
    }

    public static BaseMessage receivedStabilize() {
        if(ChordNode.predecessor == null)
            return new StabilizeResponseMessage(ChordNode.this_node.key, Macros.FAIL, BigInteger.ZERO, "0", 0);
        else return new StabilizeResponseMessage(ChordNode.this_node.key, Macros.SUCCESS, ChordNode.predecessor.key, ChordNode.predecessor.address, ChordNode.predecessor.port);
    }

    private static void sendDisconnectMessage(DisconnectMessage message) {

        for(int i = 0; i < ChordNode.num_successors; i++){
            NodeInfo successor = ChordNode.successors.get(i);
            try {
                Network.makeRequest(message,successor.address, successor.port);
                return;
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Successor is down, moving on to next successor...");
            }
        }

        System.out.println("All successors in list are currently down");
    }

    public static BaseMessage receivedDisconnectMessage(DisconnectMessage message) {
        if(!message.getSender().equals(ChordNode.this_node.key)){
            ChordNode.fingerTableRecovery(message.getKey());
            Peer.thread_executor.execute(()->BackupProtocol.recoverLostChunksReplication(message.getKey()));
            Peer.thread_executor.execute(()->sendDisconnectMessage(message));
        }
        return new MockMessage(ChordNode.this_node.key);
    }
}
