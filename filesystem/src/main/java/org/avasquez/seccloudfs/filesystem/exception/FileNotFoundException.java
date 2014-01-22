package org.avasquez.seccloudfs.filesystem.exception;

import java.io.IOException;

/**
 * Created by alfonsovasquez on 19/01/14.
 */
public class FileNotFoundException extends IOException {

    public FileNotFoundException(String message) {
        super(message);
    }

}
