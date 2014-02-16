package org.avasquez.seccloudfs.exception;

import java.io.IOException;

/**
 * Created by alfonsovasquez on 02/02/14.
 */
public class DbException extends IOException {

    public DbException(String message) {
        super(message);
    }

    public DbException(String message, Throwable cause) {
        super(message, cause);
    }

    public DbException(Throwable cause) {
        super(cause);
    }

}
