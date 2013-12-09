package org.avasquez.seccloudfs.filesystem;

/**
 * Represents the content of a file.
 *
 * @author avasquez
 */
public interface FileContent {

    /**
     * Reads the entire byte array at the specified position
     *
     * @param bytes     the byte array to store the read bytes
     * @param position  the position to start reading at
     *
     * @return the actual number of read bytes
     */
    int read(byte[] bytes, long position);

    /**
     * Writes the given bytes starting at the specified position.
     *
     * @param bytes     the bytes to write
     * @param position  the position to start writing to
     */
    void write(byte[] bytes, long position);

    /**
     * Closes all resources used by the content.
     */
    void close();

}
