package org.avasquez.seccloudfs.filesystem.db.model;

import org.jongo.marshall.jackson.oid.Id;
import org.jongo.marshall.jackson.oid.ObjectId;

import java.util.Date;

/**
 * Created by alfonsovasquez on 12/01/14.
 */
public class ContentMetadata {

    @Id
    @ObjectId
    private String id;
    private volatile long uploadedSize;
    private volatile Date lastUploadTime;
    private volatile boolean markedAsDeleted;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getUploadedSize() {
        return uploadedSize;
    }

    public void setUploadedSize(long uploadedSize) {
        this.uploadedSize = uploadedSize;
    }

    public Date getLastUploadTime() {
        return lastUploadTime;
    }

    public void setLastUploadTime(Date lastUploadTime) {
        this.lastUploadTime = lastUploadTime;
    }

    public boolean isMarkedAsDeleted() {
        return markedAsDeleted;
    }

    public void setMarkedAsDeleted(boolean markedAsDeleted) {
        this.markedAsDeleted = markedAsDeleted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ContentMetadata metadata = (ContentMetadata) o;

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
        return "ContentMetadata{" +
                "id='" + id + '\'' +
                ", uploadedSize=" + uploadedSize +
                ", lastUploadTime=" + lastUploadTime +
                ", markedAsDeleted=" + markedAsDeleted +
                '}';
    }

}
