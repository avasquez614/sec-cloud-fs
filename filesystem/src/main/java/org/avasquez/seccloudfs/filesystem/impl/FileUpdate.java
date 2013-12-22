package org.avasquez.seccloudfs.filesystem.impl;

/**
 * Holds information about a file update that needs to be uploaded to the cloud.
 *
 * @author avasquez
 */
public class FileUpdate {

    private long position;
    private int length;

    public FileUpdate(long position, int length) {
        this.position = position;
        this.length = length;
    }

    public long getPosition() {
        return position;
    }

    public int getLength() {
        return length;
    }

}
