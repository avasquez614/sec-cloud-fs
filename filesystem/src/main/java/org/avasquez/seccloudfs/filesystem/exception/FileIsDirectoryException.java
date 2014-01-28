package org.avasquez.seccloudfs.filesystem.exception;

import java.io.IOException;

/**
 * Created by alfonsovasquez on 27/01/14.
 */
public class FileIsDirectoryException extends IOException {

    public FileIsDirectoryException(String message) {
        super(message);
    }

}
