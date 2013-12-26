package org.avasquez.seccloudfs.filesystem.exception;

/**
 * Thrown when trying to delete a directory and the directory isn't empty.
 *
 * @author avasquez
 */
public class DirectoryNotEmptyException extends FileSystemException {

    public DirectoryNotEmptyException() {
    }

    public DirectoryNotEmptyException(String message) {
        super(message);
    }

    public DirectoryNotEmptyException(String message, Throwable cause) {
        super(message, cause);
    }

    public DirectoryNotEmptyException(Throwable cause) {
        super(cause);
    }

}
