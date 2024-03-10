package project.peer;

import java.math.BigInteger;
import java.util.Objects;

public class NodeInfo {
    public final BigInteger key;
    public final String address;
    public final int port;

    public NodeInfo(BigInteger key, String address, int port) {
        this.key = key;
        this.address = address;
        this.port = port;
    }

    @Override
    public String toString() {
        return "key=" + key + ", address=" + address + ", port=" + port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeInfo nodeInfo = (NodeInfo) o;
        return port == nodeInfo.port &&
                Objects.equals(key, nodeInfo.key) &&
                Objects.equals(address, nodeInfo.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, address, port);
    }
}
