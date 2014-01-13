package org.avasquez.seccloudfs.filesystem.db.model;

/**
 * Represents a truncate operation.
 *
 * @author avasquez
 */
public class TruncateOperation extends FileOperation {

    protected long size;

    protected TruncateOperation() {
        super(Type.TRUNCATE);
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

}
