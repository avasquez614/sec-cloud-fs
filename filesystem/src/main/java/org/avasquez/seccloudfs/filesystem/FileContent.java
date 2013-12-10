package org.avasquez.seccloudfs.filesystem;

import java.io.IOException;

/**
 * Represents the content of a file.
 *
 * @author avasquez
 */
public interface FileContent {

    /**
     * Reads the entire byte array beginning at the specified offset.
     *
     * @param bytes     the byte array to store the read bytes
     * @param offset    the offset to start reading at
     * @param length    the number of bytes to read
     *
     * @return the actual number of read bytes
     */
    int read(byte[] bytes, int offset, int length) throws IOException;

    /**
     * Writes the given bytes starting at the specified offset.
     *
     * @param bytes     the bytes to write
     * @param offset    the offset to start writing to
     * @param length    the number of bytes to write
     */
    void write(byte[] bytes, int offset, int length) throws IOException;

    /**
     * Closes all resources used by the content.
     */
    void close() throws IOException;

}
