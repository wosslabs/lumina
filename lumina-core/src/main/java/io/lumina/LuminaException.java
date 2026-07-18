package io.lumina;

/**
 * Base unchecked exception for Lumina framework errors.
 */
public class LuminaException extends RuntimeException {
    /**
     * @param message detail message
     */
    public LuminaException(String message) {
        super(message);
    }

    /**
     * @param message detail message
     * @param cause underlying cause
     */
    public LuminaException(String message, Throwable cause) {
        super(message, cause);
    }
}
