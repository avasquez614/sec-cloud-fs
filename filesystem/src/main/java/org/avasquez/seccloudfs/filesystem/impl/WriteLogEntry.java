package org.avasquez.seccloudfs.filesystem.impl;

import java.util.Date;

/**
 * Represents a file write operation that can be logged.
 *
 * @author avasquez
 */
public class WriteLogEntry {

    private String filePath;
    private long position;
    private long length;
    private Date date;

    public WriteLogEntry(String filePath, long position, long length, Date date) {
        this.filePath = filePath;
        this.position = position;
        this.length = length;
        this.date = date;
    }

    public String getFilePath() {
        return filePath;
    }

    public long getPosition() {
        return position;
    }

    public long getLength() {
        return length;
    }

    public Date getDate() {
        return date;
    }

}
