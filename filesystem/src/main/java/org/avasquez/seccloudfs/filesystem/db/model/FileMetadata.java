package org.avasquez.seccloudfs.filesystem.db.model;

import org.avasquez.seccloudfs.filesystem.files.User;
import org.jongo.marshall.jackson.oid.Id;
import org.jongo.marshall.jackson.oid.ObjectId;

import java.util.Date;

/**
 * The cloud file metadata. All fields are volatile since instances are normally used by multiple threads.
 *
 * @author avasquez
 */
public class FileMetadata {

    @Id
    @ObjectId
    private String id;
    private volatile boolean directory;
    private volatile String contentId;
    private volatile Date lastChangeTime;
    private volatile Date lastModifiedTime;
    private volatile Date lastAccessTime;
    private volatile User owner;
    private volatile long permissions;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isDirectory() {
        return directory;
    }

    public void setDirectory(boolean directory) {
        this.directory = directory;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public Date getLastChangeTime() {
        return lastChangeTime;
    }

    public void setLastChangeTime(Date lastChangeTime) {
        this.lastChangeTime = lastChangeTime;
    }

    public Date getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(Date lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    public Date getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(Date lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public long getPermissions() {
        return permissions;
    }

    public void setPermissions(long permissions) {
        this.permissions = permissions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FileMetadata metadata = (FileMetadata) o;

        if (!id.equals(metadata.id)) {
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
        return "FileMetadata{" +
                "id='" + id + '\'' +
                ", directory=" + directory +
                ", contentId='" + contentId + '\'' +
                ", lastChangeTime=" + lastChangeTime +
                ", lastModifiedTime=" + lastModifiedTime +
                ", lastAccessTime=" + lastAccessTime +
                ", owner=" + owner +
                ", permissions=" + permissions +
                '}';
    }

}
