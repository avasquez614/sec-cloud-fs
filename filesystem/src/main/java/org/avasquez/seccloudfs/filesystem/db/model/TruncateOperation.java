package org.avasquez.seccloudfs.filesystem.db.model;

/**
 * Represents a truncate operation.
 *
 * @author avasquez
 */
public class TruncateOperation extends FileOperation {

    protected long size;

    public TruncateOperation() {
        type = Type.TRUNCATE;
    }

    @Override
    public void setType(Type type) {
        throw new UnsupportedOperationException();
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

}
