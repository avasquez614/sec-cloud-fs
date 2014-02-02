package org.avasquez.seccloudfs.filesystem.db.model;

import org.jongo.marshall.jackson.oid.Id;
import org.jongo.marshall.jackson.oid.ObjectId;

import java.util.Date;

/**
 * Created by alfonsovasquez on 01/02/14.
 */
public class DirectoryEntry {

    @Id
    @ObjectId
    private String id;
    private String directoryId;
    private String fileName;
    private String fileId;
    private Date addedDate;

    /**
     * Private no-arg constructor, for use by frameworks like Jongo/Jackson.
     */
    private DirectoryEntry() {
    }

    public DirectoryEntry(String directoryId, String fileName, String fileId, Date addedDate) {
        this.directoryId = directoryId;
        this.fileName = fileName;
        this.fileId = fileId;
        this.addedDate = addedDate;
    }

    public DirectoryEntry(String id, String directoryId, String fileName, String fileId, Date addedDate) {
        this.id = id;
        this.directoryId = directoryId;
        this.fileName = fileName;
        this.fileId = fileId;
        this.addedDate = addedDate;
    }

    public String getId() {
        return id;
    }

    public String getDirectoryId() {
        return directoryId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileId() {
        return fileId;
    }

    public Date getAddedDate() {
        return addedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DirectoryEntry entry = (DirectoryEntry) o;

        if (!id.equals(entry.id)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "DirectoryEntry{" +
                "id='" + id + '\'' +
                ", directoryId='" + directoryId + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileId='" + fileId + '\'' +
                ", addedDate=" + addedDate +
                '}';
    }

}
