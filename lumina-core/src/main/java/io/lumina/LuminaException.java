package io.lumina;

/**
 * Base unchecked exception for Lumina framework errors.
 */
public class LuminaException extends RuntimeException {
    public LuminaException(String message) {
        super(message);
    }

    public LuminaException(String message, Throwable cause) {
        super(message, cause);
    }
}
