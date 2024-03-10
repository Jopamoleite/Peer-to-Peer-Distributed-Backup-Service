package project.message;

import java.math.BigInteger;
import java.util.ArrayList;

public class NotifyStorageMessage extends BaseMessage {
    private final ArrayList<Integer> chunk_numbers;
    private final String file_id;
    //if true the message is checking if the file is store, otherwise is notifying that is indeed storage
    private final boolean checkStorage;

    public NotifyStorageMessage(BigInteger sender, ArrayList<Integer> chunk_numbers, String file_id, boolean checkStorage) {
        super(Message_Type.NOTIFY_STORAGE, sender);
        this.chunk_numbers = chunk_numbers;
        this.file_id = file_id;
        this.checkStorage = checkStorage;
    }

    public String getFileId() {
        return file_id;
    }

    public boolean isCheckStorage() {
        return checkStorage;
    }

    public ArrayList<Integer> getChunk_numbers() {
        return chunk_numbers;
    }
}
