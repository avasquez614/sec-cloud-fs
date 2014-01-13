package org.avasquez.seccloudfs.filesystem.db.model;

/**
 * Represents a file operation.
 *
 * @author avasquez
 */
public abstract class FileOperation {

    public enum Type {
        WRITE,
        TRUNCATE,
        DELETE,
        COPY,
        MOVE;
    }

    protected String id;
    protected String path;
    protected Type type;
    protected boolean committed;

    protected FileOperation(Type type) {
        this.type = type;
        this.committed = false;
    }

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

    public boolean isCommitted() {
        return committed;
    }

    public void setCommitted(boolean committed) {
        this.committed = committed;
    }

}
