package org.avasquez.seccloudfs.filesystem.exception;

/**
 * Thrown when a file is not found.
 *
 * @author avasquez
 */
public class NotSuchFileException extends FileSystemException {

    public NotSuchFileException() {
    }

    public NotSuchFileException(String message) {
        super(message);
    }

    public NotSuchFileException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotSuchFileException(Throwable cause) {
        super(cause);
    }

}
