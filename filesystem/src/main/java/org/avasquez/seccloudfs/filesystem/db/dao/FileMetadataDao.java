package org.avasquez.seccloudfs.filesystem.db.dao;

import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;

import java.util.List;

/**
 * Data Access Object for {@link org.avasquez.seccloudfs.filesystem.db.model.FileMetadata}s.
 */
public interface FileMetadataDao {

    /**
     * Returns the file metadata with the specified path in the database.
     *
     * @param path  the path of the file metadata to look for
     *
     * @return the file metadata, or null if not found
     */
    FileMetadata findByPath(String path);

    /**
     * Returns the children metadata of the directory specified by the path.
     *
     * @param path  the path of the directory
     *
     * @return the children metadata, or null if directory metadata not found
     */
    List<FileMetadata> findChildren(String path);

    /**
     * Saves the file metadata in the database.
     *
     * @param metadata  the file metadata to save
     */
    void save(FileMetadata metadata);

    /**
     * Deletes the file metadata for the specified ID in the database.
     *
     * @param path    the path the file metadata to delete
     */
    void delete(String path);

}
