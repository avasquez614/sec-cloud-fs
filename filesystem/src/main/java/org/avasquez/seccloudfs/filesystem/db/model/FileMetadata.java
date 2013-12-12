package org.avasquez.seccloudfs.filesystem.db.model;

import java.util.BitSet;
import java.util.Date;

/**
 * The file metadata.
 *
 * @author avasquez
 */
public class FileMetadata {

    private String id;
    private String path;
    private long size;
    private Date lastModified;
    private Date lastAccess;
    private long chunkSize;
    private BitSet cachedChunks;

    /**
     * Returns the file ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the file path.
     */
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Sets the file ID.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns length of the file in bytes.
     */
    public long getSize() {
        return size;
    }

    /**
     * Sets the length of the file in bytes.
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * Returns the date of the last modification.
     */
    public Date getLastModified() {
        return lastModified;
    }

    /**
     * Sets the date of the last modification.
     */
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Returns the date of the last access.
     */
    public Date getLastAccess() {
        return lastAccess;
    }

    /**
     * Sets the date of the last access.
     */
    public void setLastAccess(Date lastAccess) {
        this.lastAccess = lastAccess;
    }

    /**
     * Returns the chunk size in bytes.
     */
    public long getChunkSize() {
        return chunkSize;
    }

    /**
     * Sets the chunk size for the file.
     */
    public void getChunkSize(long chunkSize) {
        this.chunkSize = chunkSize;
    }

    /**
     * Returns the ID for the specified chunk.
     */
    public String getChunkId(int chunkIdx) {
        return getId() + "$" + chunkIdx;
    }

    /**
     * Returns the bit mask of chunks of the file that are cached locally.
     */
    public BitSet getCachedChunks() {
        return cachedChunks;
    }

    /**
     * Sets the bit mask of chunks of the file that are cached locally.
     */
    public void setCachedChunks(BitSet cachedChunks) {
        this.cachedChunks = cachedChunks;
    }

}
