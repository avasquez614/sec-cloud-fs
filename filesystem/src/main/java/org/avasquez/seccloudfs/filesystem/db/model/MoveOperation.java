package org.avasquez.seccloudfs.filesystem.db.model;

/**
 * Represents a move operation.
 *
 * @author avasquez
 */
public class MoveOperation extends CopyOperation {

    public MoveOperation() {
        this.type = Type.MOVE;
    }

}
