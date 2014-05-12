package org.avasquez.seccloudfs.erasure;

/**
 * Root exception for erasure based errors.
 *
 * @author avasquez
 */
public class ErasureException extends Exception {

    public ErasureException(String message) {
        super(message);
    }

    public ErasureException(String message, Throwable cause) {
        super(message, cause);
    }

}
