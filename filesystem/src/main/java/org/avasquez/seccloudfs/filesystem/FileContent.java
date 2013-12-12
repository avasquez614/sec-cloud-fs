package org.avasquez.seccloudfs.filesystem;

import java.io.IOException;

/**
 * Represents the content of a file.
 *
 * @author avasquez
 */
public interface FileContent {

    /**
     * Reads the entire byte array beginning at the specified position in the file.
     *
     * @param bytes     the byte array to store the read bytes
     * @param position  the position to start reading at
     *
     * @return the actual number of read bytes
     */
    int read(byte[] bytes, int position) throws IOException;

    /**
     * Writes the given bytes starting at the specified position in the file.
     *
     * @param bytes     the bytes to write
     * @param position  the position to start writing to
     */
    void write(byte[] bytes, int position) throws IOException;

    /**
     * Closes all resources used by the content.
     */
    void close() throws IOException;

}
