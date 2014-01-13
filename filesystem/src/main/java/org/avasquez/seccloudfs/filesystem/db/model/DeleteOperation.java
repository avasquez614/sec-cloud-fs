package org.avasquez.seccloudfs.filesystem.db.model;

/**
 * Represents a delete operation.
 *
 * @author avasquez
 */
public class DeleteOperation extends FileOperation {

    protected DeleteOperation() {
        super(Type.DELETE);
    }

}
