package org.avasquez.seccloudfs.filesystem.db.model;

import java.util.Date;

/**
 * Represents a file operation.
 *
 * @author avasquez
 */
public class FileOperation {

    public enum Type {
        WRITE,
        TRUNCATE,
        DELETE;
    }

    protected String id;
    protected String path;
    protected Type type;
    protected Date beginTime;
    protected Date endTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Date getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(Date beginTime) {
        this.beginTime = beginTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

}
