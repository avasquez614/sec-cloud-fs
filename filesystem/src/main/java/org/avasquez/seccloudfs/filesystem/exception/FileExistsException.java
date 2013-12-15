package org.avasquez.seccloudfs.filesystem.exception;

/**
 * Thrown when trying to create a file or directory and it already exists.
 *
 * @author avasquez
 */
public class FileExistsException extends FileSystemException {

    public FileExistsException() {
    }

    public FileExistsException(String message) {
        super(message);
    }

}
