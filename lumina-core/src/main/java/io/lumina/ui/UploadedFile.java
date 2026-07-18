package io.lumina.ui;

/**
 * Uploaded file available during the run that received it.
 *
 * @param fileName original file name
 * @param contentType MIME type if known
 * @param bytes file bytes
 */
public record UploadedFile(String fileName, String contentType, byte[] bytes) {
    /**
     * Creates an uploaded file with a defensive copy of the byte array.
     */
    public UploadedFile {
        bytes = bytes == null ? new byte[0] : bytes.clone();
    }
}
