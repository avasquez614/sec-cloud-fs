package org.avasquez.seccloudfs.filesystem.exception;

/**
 * Root exception for all related filesystem stuff.
 *
 * @author avasquez
 */
public class FileSystemException extends Exception {

    public FileSystemException() {
    }

    public FileSystemException(String message) {
        super(message);
    }

    public FileSystemException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileSystemException(Throwable cause) {
        super(cause);
    }

}
