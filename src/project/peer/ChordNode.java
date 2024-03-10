package project.peer;

import project.Macros;
import project.message.StabilizeResponseMessage;
import project.protocols.ConnectionProtocol;
import project.protocols.StorageRestoreProtocol;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class ChordNode {
    private static final int m = 64;

    public static final int num_successors = 3;

    public static NodeInfo this_node;
    public static NodeInfo predecessor;

    public static ConcurrentHashMap<Integer, NodeInfo> finger_table = new ConcurrentHashMap<>();
    public static CopyOnWriteArrayList<NodeInfo> successors = new CopyOnWriteArrayList<>();

    public ChordNode(int port) throws IOException, NoSuchAlgorithmException {
        predecessor = null;
        String address;
        try(final DatagramSocket socket = new DatagramSocket()){
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            address = socket.getLocalAddress().getHostAddress();
        }catch(Exception e){
            address = InetAddress.getLocalHost().getHostAddress();
        }
        this_node = new NodeInfo(generateKey(address + ":" + port), address, port);
        Network.initiateServerSockets(this_node.port);
        initializeFingerTable();
        intializeSuccessorList();

        start();
    }

    public ChordNode(int port, String neighbour_address, int neighbour_port) throws IOException, NoSuchAlgorithmException {
        predecessor = null;
        String address;
        try(final DatagramSocket socket = new DatagramSocket()){
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            address = socket.getLocalAddress().getHostAddress();
        }catch(Exception e){
            address = InetAddress.getLocalHost().getHostAddress();
        }
        this_node = new NodeInfo(generateKey(address + ":" + port), address, port);
        Network.initiateServerSockets(this_node.port);
        initializeFingerTable();
        intializeSuccessorList();
        ConnectionProtocol.connectToNetwork(neighbour_address, neighbour_port);

        start();
    }

    private void start() {
        Peer.thread_executor.execute(Network::run);
        Peer.scheduled_executor.scheduleAtFixedRate(this::verifyState, 3, 10, TimeUnit.SECONDS);

        System.out.println("Peer " + Peer.id + " running in address " + this_node.address + " and port " + this_node.port +
                "\n( key: " + this_node.key.toString() + " )");
    }

    public static BigInteger generateKey(String data) throws NoSuchAlgorithmException {
        BigInteger maximum = new BigInteger("2").pow(m);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return new BigInteger(1, hash).mod(maximum);
    }

    private void verifyState(){
        Peer.thread_executor.execute(this::stabilize);
        Peer.thread_executor.execute(this::verifyPredecessor);
        Peer.thread_executor.execute(this::updateFingerTable);
    }

    private void stabilize() {
        if(getSuccessorNode() != null){
            NodeInfo previous_successor = getSuccessorNode();
            StabilizeResponseMessage new_successor = ConnectionProtocol.stabilize(previous_successor, false);

            if(new_successor == null) {
                addSuccessor(successors.get(1));
                successors.add(0,successors.get(1));
                successors.add(1, successors.get(2));
                successors.add(2, ChordNode.findSuccessor(successorHelper(successors.get(1).key)));
                if(!ConnectionProtocol.notifySuccessor()){
                    addSuccessor(successors.get(1));
                    successors.add(0,successors.get(1));
                    successors.add(1, successors.get(2));
                    successors.add(2, ChordNode.findSuccessor(successorHelper(successors.get(1).key)));
                }
            }

            previous_successor = getSuccessorNode();
            new_successor = ConnectionProtocol.stabilize(previous_successor, true);

            if(new_successor == null){
                return;
            }

            if (new_successor.getStatus().equals(Macros.SUCCESS) &&
                    (!this_node.key.equals(new_successor.getSender()) && isKeyBetween(new_successor.getKey(), this_node.key, previous_successor.key))){
                finger_table.replace(1, new NodeInfo(new_successor.getKey(), new_successor.getAddress(), new_successor.getPort()));
            }

            boolean successor_updated = !finger_table.get(1).key.equals(previous_successor.key);

            if(!ConnectionProtocol.notifySuccessor()){
                if(successor_updated){
                    finger_table.replace(1, previous_successor);
                }
            }else{
                if(successor_updated){
                    successors.add(2, successors.get(1));
                    successors.add(1, successors.get(0));
                    successors.add(0,ChordNode.getSuccessorNode());
                }
            }

            NodeInfo successor2 = findSuccessor(successorHelper(ChordNode.getSuccessorNode().key));
            NodeInfo successor3 = findSuccessor(successorHelper(successor2.key));
            ChordNode.successors.add(0, ChordNode.getSuccessorNode());
            ChordNode.successors.add(1, successor2);
            ChordNode.successors.add(2, successor3);
        }

        if(predecessor != null)
            System.out.println("Predecessor: " + predecessor.key);
        System.out.println("Current Node: " + this_node.key);
        System.out.println("Successor: " + getSuccessorNode().key);
        for(int i = 1; i < num_successors; i++){
            System.out.println("Successor " + (i+1) + ": " + successors.get(i).key);
        }
        for(int i = 10; i < m; i+=10){
            System.out.println("Finger " + i + ": " + finger_table.get(i).key);
        }
        for(int i = 61; i <= m; i++){
            System.out.println("Finger " + i + ": " + finger_table.get(i).key);
        }

    }

    private void verifyPredecessor() {
        if(!ConnectionProtocol.checkPredecessor())
            predecessor = null;
    }

    private void intializeSuccessorList(){
        for(int i = 0; i < num_successors; i++){
            successors.add(this_node);
        }
    }

    private void initializeFingerTable(){
        for(int i=1; i <= m; i++)
            finger_table.put(i, this_node);
    }

    private void updateFingerTable() {
        for(int i=2; i <= m; i++){
            BigInteger key = this_node.key.add(new BigInteger("2").pow(i-1)).mod(new BigInteger("2").pow(m));
            int entry = i;
            Runnable task = ()->updateTableEntry(entry, key);
            Peer.thread_executor.execute(task);
        }
    }

    public static void fingerTableRecovery(BigInteger key) {
        if(finger_table.get(m).key.equals(key))
            finger_table.replace(m, this_node);

        for(int i=m-1; i > 0; i--){
            if(finger_table.get(i).key.equals(key)){
                finger_table.replace(i, finger_table.get(i+1));
            }
        }

        if(getSuccessorNode().equals(this_node)){
            System.out.println("There are no more nodes where this peer can grab on to");
            //Não é necessariamente necessário fechar o node, pode aparecer outro para se ligar a este
        }
    }

    private void updateTableEntry(int entry, BigInteger key){
        NodeInfo node = findSuccessor(key);
        if(node == null)
            return;
        finger_table.put(entry, node);
    }

    public static BigInteger successorHelper(BigInteger successor){
        return successor.add(BigInteger.ONE).mod(BigInteger.TWO.pow(m));
    }

    public static void setSuccessor(String chunk) {
        List<String> node_info = Arrays.asList(chunk.split(":"));
        NodeInfo successor =  new NodeInfo(new BigInteger(node_info.get(0).trim()), node_info.get(1).trim(), Integer.parseInt(node_info.get(2).trim()));
        finger_table.put(1, successor);
        successors.add(0, successor);
    }

    public static byte[] getSuccessor() {
        NodeInfo node = this_node;

        if(finger_table.size() > 0){
            node = getSuccessorNode();
        }

        return (node.key + ":" + node.address + ":" + node.port).getBytes();
    }


    public static NodeInfo getSuccessorNode(){
        return finger_table.get(1);
    }

    public static void addSuccessor(NodeInfo successor) {
        finger_table.put(1, successor);
    }

    public static String setPredecessor(BigInteger key, String address, int port) {
        if(predecessor == null || key.equals(predecessor.key) || isKeyBetween(key, predecessor.key, this_node.key)) {
            predecessor = new NodeInfo(key, address, port);
            return Macros.SUCCESS;
        }
        else return Macros.FAIL;
    }

    public static NodeInfo findSuccessor(BigInteger successor){
        if(this_node.key.equals(successor)) {
            return this_node;
        }

        NodeInfo local_successor = getSuccessorNode();

        if(isKeyBetween(successor, this_node.key, local_successor.key)){
            return local_successor;
        }

        NodeInfo preceding_finger = closestPrecedingNode(successor);

        return ConnectionProtocol.findSuccessor(successor, preceding_finger);
    }

    public static NodeInfo findPredecessor(BigInteger successor){
        if(finger_table.size() == 0) {
            return this_node;
        }
        else if(isKeyBetween(successor, this_node.key, getSuccessorNode().key)){
            return this_node;
        }

        NodeInfo preceding_finger = closestPrecedingNode(successor);

        if(preceding_finger.key.equals(this_node.key)){
            return this_node;
        }

        return ConnectionProtocol.findPredecessor(successor, preceding_finger);
    }

    public static NodeInfo closestPrecedingNode(BigInteger key) {
        for (int n = finger_table.size(); n >= 1; n--) {
            NodeInfo finger = finger_table.get(n);
            if ( finger != null && isKeyBetween(finger.key, this_node.key, key))
                return finger;
        }
        return this_node;
    }

    public static NodeInfo findPreviousFinger(BigInteger key){
        for (int n = finger_table.size(); n >= 1; n--) {
            NodeInfo finger = finger_table.get(n);
            if ( finger != null && isKeyBetween(finger.key, this_node.key, key) && !finger.key.equals(key))
                return finger;
        }
        return this_node;
    }

    //Returns true if key is between lowerBound and upperBound, taking into account chord nodes are in a circle (lowerBound can have a higher value than upperBound)
    public static boolean isKeyBetween(BigInteger key, BigInteger lowerBound, BigInteger upperBound){
        if(lowerBound.compareTo(upperBound) >= 0){
            return (key.compareTo(lowerBound) > 0) || (key.compareTo(upperBound) <= 0);
        }else{
            return (key.compareTo(lowerBound) > 0) && (key.compareTo(upperBound) <= 0);
        }
    }
}
