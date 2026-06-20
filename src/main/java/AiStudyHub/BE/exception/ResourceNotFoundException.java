package AiStudyHub.BE.exception;

/**
 * Exception thrown when a requested resource (like a document) is not found.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
