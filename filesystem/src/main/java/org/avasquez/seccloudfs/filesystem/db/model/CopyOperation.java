package org.avasquez.seccloudfs.filesystem.db.model;

/**
 * Represents a copy operation.
 *
 * @author avasquez
 */
public class CopyOperation extends FileOperation {

    protected String targetPath;

    protected CopyOperation() {
        super(Type.COPY);
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

}
