package org.avasquez.seccloudfs.filesystem.exception;

import java.io.IOException;

/**
 * Thrown when trying to create a file or directory and it already exists.
 *
 * @author avasquez
 */
public class FileExistsException extends IOException {

    public FileExistsException(String message) {
        super(message);
    }

}
