package project.message;

import java.math.BigInteger;
import java.util.ArrayList;

public class StorageResponseMessage extends BaseMessage {
    private ArrayList<Integer> success_chunk_numbers;
    private ArrayList<Integer> not_success_chunk_numbers;
    private final String file_id;
    //if true the message is checking if the file is store, otherwise is notifying that is indeed storage
    private final boolean store;

    public StorageResponseMessage(BigInteger sender, ArrayList<Integer> chunk_numbers, String file_id, boolean store, boolean successful) {
        super(Message_Type.STORAGE_RESPONSE, sender);
        if(successful) {
            success_chunk_numbers = chunk_numbers;
            not_success_chunk_numbers = null;
        } else {
            not_success_chunk_numbers = chunk_numbers;
            success_chunk_numbers = null;
        }

        this.file_id = file_id;
        this.store = store;
    }

    public StorageResponseMessage(BigInteger sender, ArrayList<Integer> found_chunk_numbers,  ArrayList<Integer> not_found_chunk_numbers, String file_id, boolean store) {
        super(Message_Type.STORAGE_RESPONSE, sender);
        this.success_chunk_numbers = found_chunk_numbers;
        this.not_success_chunk_numbers = not_found_chunk_numbers;
        this.file_id = file_id;
        this.store = store;

    }

    public ArrayList<Integer> get_foundChunk_number() {
        return success_chunk_numbers;
    }

    public ArrayList<Integer> get_not_foundChunk_number() {
        return not_success_chunk_numbers;
    }

    public String getFile_id() {
        return file_id;
    }


    public boolean isStore() {
        return store;
    }
}
