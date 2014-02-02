package org.avasquez.seccloudfs.filesystem.db.dao;

import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;

/**
 * Data Access Object for {@link org.avasquez.seccloudfs.filesystem.db.model.FileMetadata}s.
 */
public interface FileMetadataDao {

    /**
     * Returns the root metadata.
     */
    FileMetadata getRoot();

    /**
     * Returns the file metadata with the specified ID in the database.
     *
     * @param id  the ID of the file metadata to look for
     *
     * @return the file metadata, or null if not found
     */
    FileMetadata find(String id);

    /**
     * Inserts the file metadata in the database.
     *
     * @param metadata  the metadata to insert
     */
    void insert(FileMetadata metadata);

    /**
     * Saves the specified metadata in the database.
     *
     * @param metadata  the metadata to update
     */
    void save(FileMetadata metadata);

    /**
     * Deletes the file metadata for the specified ID in the database.
     *
     * @param id    the ID the file metadata to delete
     */
    void delete(String id);

}
