package org.avasquez.seccloudfs.filesystem;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Represents the content of a file.
 *
 * @author avasquez
 */
public interface FileContent {

    /**
     * Reads the entire byte array beginning at the specified position in the file.
     *
     * @param buffer    the byte buffer to store the read bytes
     * @param position  the position to start reading at
     *
     * @return the actual number of read buffer
     */
    int read(ByteBuffer buffer, int position) throws IOException;

    /**
     * Writes the given bytes starting at the specified position in the file.
     *
     * @param buffer    the byte buffer to write to
     * @param position  the position to start writing to
     */
    void write(ByteBuffer buffer, int position) throws IOException;

    /**
     * Closes all resources used by the content.
     */
    void close() throws IOException;

}
