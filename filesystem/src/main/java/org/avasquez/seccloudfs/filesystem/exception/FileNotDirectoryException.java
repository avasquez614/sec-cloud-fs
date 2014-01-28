package org.avasquez.seccloudfs.filesystem.exception;

import java.io.IOException;

/**
 * Created by alfonsovasquez on 25/01/14.
 */
public class FileNotDirectoryException extends IOException {

    public FileNotDirectoryException(String message) {
        super(message);
    }

}
