package project.chunk;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;

public class StoredChunks implements Serializable {
    private BigInteger owner;
    private int replication_degree;
    private ArrayList<Integer> chunk_numbers;

    public StoredChunks(BigInteger owner, Integer replication_degree, ArrayList<Integer> chunks_stored) {
        this.owner = owner;
        this.replication_degree = replication_degree;
        this.chunk_numbers = chunks_stored;
    }

    public BigInteger getOwner() {
        return owner;
    }

    public int getReplication_degree() {
        return replication_degree;
    }

    public ArrayList<Integer> getChunkNumbers(){
        return chunk_numbers;
    }

    public void addChunkNumber(int chunk_no){
        chunk_numbers.add(chunk_no);
    }

    public void removeChunkNumber(Integer chunk_number) {
        chunk_numbers.remove(chunk_number);
    }
}
