package project.chunk;

public class Chunk {
    public final int chunk_no;
    public final byte[] content;
    public final int size;

    public Chunk(int chunk_no, byte[] content, int size) {
        this.chunk_no = chunk_no;
        this.content = content;
        this.size = size;
    }
}
