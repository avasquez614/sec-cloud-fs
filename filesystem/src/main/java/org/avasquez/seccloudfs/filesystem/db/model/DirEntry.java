package org.avasquez.seccloudfs.filesystem.db.model;

import java.util.Date;

/**
 * Created by alfonsovasquez on 26/01/14.
 */
public class DirEntry {

    private String fileId;
    private Date createdDate;

    /**
     * Private no-arg constructor, for use by frameworks like Jongo/Jackson.
     */
    private DirEntry() {
    }

    public DirEntry(String fileId, Date createdDate) {
        this.fileId = fileId;
        this.createdDate = createdDate;
    }

    public String getFileId() {
        return fileId;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DirEntry entry = (DirEntry) o;

        if (!createdDate.equals(entry.createdDate)) {
            return false;
        }
        if (!fileId.equals(entry.fileId)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = fileId.hashCode();
        result = 31 * result + createdDate.hashCode();
        return result;
    }

}
