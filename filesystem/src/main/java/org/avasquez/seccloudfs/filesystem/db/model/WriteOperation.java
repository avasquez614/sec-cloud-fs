package org.avasquez.seccloudfs.filesystem.db.model;

/**
 * Represents a write operation.
 *
 * @author avasquez
 */
public class WriteOperation extends FileOperation {

    protected long position;
    protected long length;

    public WriteOperation() {
        super(Type.WRITE);
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

}
