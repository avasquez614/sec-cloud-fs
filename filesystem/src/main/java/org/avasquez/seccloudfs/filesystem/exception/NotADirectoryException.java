package org.avasquez.seccloudfs.filesystem.exception;

import java.io.IOException;

/**
 * Created by alfonsovasquez on 25/01/14.
 */
public class NotADirectoryException extends IOException {

    public NotADirectoryException(String message) {
        super(message);
    }

}
