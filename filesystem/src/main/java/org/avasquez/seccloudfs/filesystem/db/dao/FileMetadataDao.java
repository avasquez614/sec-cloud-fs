package org.avasquez.seccloudfs.filesystem.db.dao;

import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;

/**
 * Data Access Object for {@link org.avasquez.seccloudfs.filesystem.db.model.FileMetadata}s.
 */
public interface FileMetadataDao {

    /**
     * Searches the file for the specified ID in the database.
     *
     * @param id    the ID of the file to look for
     *
     * @return the file, or null if not found
     */
    FileMetadata findById(String id);

    /**
     * Saves the file in the database.
     *
     * @param file  the file to save
     */
    void save(FileMetadata file);

    /**
     * Deletes the file for the specified ID in the database.
     *
     * @param id    the ID of the file to delete
     */
    void delete(String id);

}
