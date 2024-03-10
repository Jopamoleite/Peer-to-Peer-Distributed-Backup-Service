package project.store;

import project.Pair;

import java.io.Serializable;

public class FileInfo implements Serializable {
    private final String file_name;
    private final String file_id;
    private final Integer number_of_chunks;
    private final String file_path;

    public FileInfo(String file_name, String file_id, Integer number_of_chunks, String file_path) {
        this.file_name = file_name;
        this.file_id = file_id;
        this.number_of_chunks = number_of_chunks;
        this.file_path = file_path;
    }

    public String getFileName() {
        return file_name;
    }

    public String getFileId() {
        return file_id;
    }

    public Integer getNumberOfChunks() {
        return number_of_chunks;
    }

    public String getFilePath() {
        return file_path;
    }
}
