package org.avasquez.seccloudfs.filesystem.db.model;

import java.util.BitSet;
import java.util.Date;

/**
 * The cloud file metadata.
 *
 * @author avasquez
 */
public class FileMetadata {

    private String path;
    private String parent;
    private boolean directory;
    private long size;
    private Date lastModified;
    private Date lastAccess;
    private String contentId;
    private long chunkSize;
    private BitSet cachedChunks;

    /**
     * Returns the file's path in the virtual filesystem.
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the file's path in the virtual filesystem.
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Returns the parent path.
     */
    public String getParent() {
        return parent;
    }

    /**
     * Sets the path of the parent directory.
     */
    public void setParent(String parent) {
        this.parent = parent;
    }

    /**
     * Returns true if the file is a directory, false otherwise.
     */
    public boolean isDirectory() {
        return directory;
    }

    /**
     * Sets whether the file is a directory or not.
     */
    public void setDirectory(boolean directory) {
        this.directory = directory;
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
     * Returns the ID of the file content used for caching and storage.
     */
    public String getContentId() {
        return contentId;
    }

    /**
     * Sets the ID of the file content used for caching and storage.
     */
    public void setContentId(String contentId) {
        this.contentId = contentId;
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
    public void setChunkSize(long chunkSize) {
        this.chunkSize = chunkSize;
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

    /**
     * Returns the storage name of the specified chunk.
     *
     * @param idx  the index of the chunk
     */
    public String getChunkName(int idx) {
        return getContentId() + "$" + idx;
    }

}
