package org.avasquez.seccloudfs.filesystem;

import java.util.BitSet;

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
     * Returns size of the file in bytes.
     */
    long getSize();

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
