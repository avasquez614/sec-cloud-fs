package org.avasquez.seccloudfs.filesystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Represents the content of a file. Not multi-thread safe, a different instance should be used by each thread.
 *
 * @author avasquez
 */
public interface FileContent extends ReadableByteChannel, WritableByteChannel {

    /**
     * Returns the current position for read/write.
     *
     * @return the current position
     */
    long getPosition() throws IOException;

    /**
     * Sets the position for read/write (when no specific position for the read/write is given).
     *
     * @param position   the new position
     */
    void setPosition(long position) throws IOException;

    /**
     * Reads the entire byte array beginning at the specified position in the file.
     *
     * @param dst    the byte buffer to store the read bytes
     * @param position  the position to start reading at
     *
     * @return the actual number of read buffer
     */
    int read(ByteBuffer dst, long position) throws IOException;

    /**
     * Writes the given bytes starting at the specified position in the file.
     *
     * @param src    the byte buffer to write to
     * @param position  the position to start writing to
     */
    int write(ByteBuffer src, long position) throws IOException;

    /**
     * Closes all resources used by the content.
     */
    void close() throws IOException;

}
