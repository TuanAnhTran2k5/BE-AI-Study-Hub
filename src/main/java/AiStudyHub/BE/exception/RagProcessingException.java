package AiStudyHub.BE.exception;

/**
 * Exception thrown when text extraction, splitting, or embedding generation fails.
 */
public class RagProcessingException extends RuntimeException {
    public RagProcessingException(String message) {
        super(message);
    }

    public RagProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
