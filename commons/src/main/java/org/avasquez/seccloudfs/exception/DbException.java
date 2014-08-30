package org.avasquez.seccloudfs.exception;

/**
 * Created by alfonsovasquez on 02/02/14.
 */
public class DbException extends Exception {

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
