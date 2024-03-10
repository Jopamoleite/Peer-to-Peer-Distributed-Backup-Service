package project.message;

/**
 * USed if the file has a size greater than the one "possible" in this project
 */
public class InvalidMessageException extends Exception {
    private String message;

    public InvalidMessageException(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


}