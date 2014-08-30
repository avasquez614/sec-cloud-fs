package org.avasquez.seccloudfs.erasure;

/**
 * Thrown when an encoding error occurs.
 *
 * @author avasquez
 */
public class EncodingException extends ErasureException {

    public EncodingException(String message) {
        super(message);
    }

    public EncodingException(String message, Throwable cause) {
        super(message, cause);
    }

}
