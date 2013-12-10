package org.avasquez.seccloudfs.filesystem;

import java.util.BitSet;
import java.util.Date;

/**
 * Simple representation of a file in the virtual filesystem.
 *
 * @author avasquez
 */
public interface File {

    /**
     * Returns the file ID.
     */
    String getId();

    /**
     * Returns length of the file in bytes.
     */
    long getSize();

    /**
     * Sets the length of the file in bytes.
     *
     * @param size  the new size
     */
    void setSize(long size);

    /**
     * Returns the date of the last modification.
     */
    Date getLastModified();

    /**
     * Sets the date of the last modification.
     *
     * @param lastModified  the new last modification date
     */
    void setLastModified(Date lastModified);

    /**
     * Returns the date of the last access.
     */
    Date getLastAccess();

    /**
     * Sets the date of the last access.
     *
     * @param lastAccess  the new last access date
     */
    void setLastAccess(Date lastAccess);

    /**
     * Returns the file content.
     */
    FileContent getFileContent();

    /**
     * Returns the chunk size in bytes.
     */
    long getChunkSize();

    /**
     * Returns the ID for the specified chunk.
     */
    String getChunkId(int chunkIdx);

    /**
     * Returns the bit mask of chunks of the file that are available locally.
     */
    BitSet getAvailableChunks();

    /**
     * Sets the bit mask of chunks of the file that are available locally.
     *
     * @param availableChunks   the bit mask of new available chunks
     */
    void setAvailableChunks(BitSet availableChunks);

}
