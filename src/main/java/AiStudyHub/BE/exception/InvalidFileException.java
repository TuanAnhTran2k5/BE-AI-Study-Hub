package AiStudyHub.BE.exception;

/**
 * Exception thrown when an uploaded file is invalid (e.g., wrong format or corrupted content).
 */
public class InvalidFileException extends RuntimeException {
    public InvalidFileException(String message) {
        super(message);
    }
}
