package io.lumina;

/**
 * Base unchecked exception for Lumina framework errors.
 */
public class LuminaException extends RuntimeException {
    /**
     * Creates a Lumina exception with the given detail message.
     *
     * @param message detail message
     */
    public LuminaException(String message) {
        super(message);
    }

    /**
     * Creates a Lumina exception with the given detail message and cause.
     *
     * @param message detail message
     * @param cause underlying cause
     */
    public LuminaException(String message, Throwable cause) {
        super(message, cause);
    }
}
