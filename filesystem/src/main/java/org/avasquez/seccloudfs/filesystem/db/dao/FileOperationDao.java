package org.avasquez.seccloudfs.filesystem.db.dao;

import org.avasquez.seccloudfs.filesystem.db.model.FileOperation;

/**
 * Data access object for {@link org.avasquez.seccloudfs.filesystem.db.model.FileOperation}s.
 *
 * @author avasquez
 */
public interface FileOperationDao {

    /**
     * Returns the file operation for the specified ID.
     *
     * @param id    the ID of the file operation
     *
     * @return the file operation, or null if not found.
     */
    FileOperation findById(String id);

    /**
     * Inserts the specified operation in the database.
     *
     * @param operation the operation to insert
     */
    void insert(FileOperation operation);

    /**
     * Updates the specified operation in the database
     *
     * @param operation the operation to update
     */
    void update(FileOperation operation);

}
