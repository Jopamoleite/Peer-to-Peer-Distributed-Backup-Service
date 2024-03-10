package project;

public class InvalidFileException extends Exception {

    public InvalidFileException(String error_message) {
        System.err.println(error_message);
    }
}
