package org.avasquez.seccloudfs.filesystem.exception;

import java.io.IOException;

/**
 * Created by alfonsovasquez on 21/01/14.
 */
public class UnexpectedFileTypeException extends IOException {

    public UnexpectedFileTypeException(String message) {
        super(message);
    }
}
