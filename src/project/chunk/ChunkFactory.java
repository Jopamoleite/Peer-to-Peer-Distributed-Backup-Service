package project.chunk;

import project.Macros;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

import static java.util.Arrays.copyOfRange;

public class ChunkFactory {
    public static ArrayList<Chunk> produceChunks(File file, int replication_degree) {
        ArrayList<Chunk> chunks = new ArrayList<>();

        int chunk_no = 0;

        byte[] buffer = new byte[Macros.CHUNK_MAX_SIZE];

        try(BufferedInputStream stream = new BufferedInputStream(new FileInputStream(file))) {
            int size;
            while((size = stream.read(buffer)) > 0){
                Chunk chunk = new Chunk(chunk_no, Arrays.copyOf(buffer, size), size);

                chunks.add(chunk);

                chunk_no++;

                buffer = new byte[Macros.CHUNK_MAX_SIZE];
            }
            //check if needs 0 size chunk
            if(chunks.get(chunks.size() - 1).size == Macros.CHUNK_MAX_SIZE) {
                // If the file size is a multiple of the chunk size, the last chunk has size 0.
                chunks.add(new Chunk(chunks.size(), new byte[0], 0));
            }
        } catch (IOException e) {
        }

        return chunks;
    }

    public static Chunk retrieveChunk(File file, int chunk_no) {
        int i = 0;

        byte[] buffer = new byte[Macros.CHUNK_MAX_SIZE];

        try(BufferedInputStream stream = new BufferedInputStream(new FileInputStream(file))) {
            int size;
            while((size = stream.read(buffer)) > 0){
                if(chunk_no == i){
                    return new Chunk(chunk_no, Arrays.copyOf(buffer, size), size);
                }

                i++;

                buffer = new byte[Macros.CHUNK_MAX_SIZE];
            }

            if(chunk_no == i+1) {
                return new Chunk(chunk_no, new byte[0], 0);
            }
        } catch (IOException e) {
        }

        return null;
    }
}
