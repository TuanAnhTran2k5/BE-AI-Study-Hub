package AiStudyHub.BE.exception;

/**
 * Exception thrown when there is a connection or operation failure with the vector database.
 */
public class VectorStoreException extends RuntimeException {
    public VectorStoreException(String message) {
        super(message);
    }

    public VectorStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
