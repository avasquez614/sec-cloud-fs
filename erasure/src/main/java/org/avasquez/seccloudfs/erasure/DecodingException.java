package org.avasquez.seccloudfs.erasure;

/**
 * Thrown when a decoding error occurs.
 *
 * @author avasquez
 */
public class DecodingException extends ErasureException {

    public DecodingException(String message) {
        super(message);
    }

    public DecodingException(String message, Throwable cause) {
        super(message, cause);
    }

}
