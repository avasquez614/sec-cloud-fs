package org.avasquez.seccloudfs.filesystem.impl;

/**
 * Holds information about a file update that needs to be uploaded to the cloud.
 *
 * @author avasquez
 */
public class FileUpdate {

    private long position;
    private long length;
    private boolean delete;

    public FileUpdate(long position, long length, boolean delete) {
        this.position = position;
        this.length = length;
        this.delete = delete;
    }

    public long getPosition() {
        return position;
    }

    public long getLength() {
        return length;
    }

    public boolean isDelete() {
        return delete;
    }
}
