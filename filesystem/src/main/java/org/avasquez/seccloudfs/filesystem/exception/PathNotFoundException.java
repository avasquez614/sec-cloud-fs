package org.avasquez.seccloudfs.filesystem.exception;

/**
 * Thrown when a file path is not found.
 *
 * @author avasquez
 */
public class PathNotFoundException extends FileSystemException {

    public PathNotFoundException() {
    }

    public PathNotFoundException(String message) {
        super(message);
    }

    public PathNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public PathNotFoundException(Throwable cause) {
        super(cause);
    }

}
