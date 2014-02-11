package org.avasquez.seccloudfs.filesystem.db.repos;

import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;

/**
 * Data Access Object for {@link org.avasquez.seccloudfs.filesystem.db.model.FileMetadata}s.
 */
public interface FileMetadataRepository {

    /**
     * Returns the file metadata with the specified ID in the database.
     *
     * @param id  the ID of the file metadata to look for
     *
     * @return the file metadata, or null if not found
     */
    FileMetadata find(String id) throws DbException;

    /**
     * Returns the metadata of all files, sorted by descending last access time.
     *
     * @return the sorted file metadata list
     */
    Iterable<FileMetadata> findAllSortedByDescLastAccessTime() throws DbException;

    /**
     * Inserts the file metadata in the database.
     *
     * @param metadata  the metadata to insert
     */
    void insert(FileMetadata metadata) throws DbException;

    /**
     * Saves the specified metadata in the database.
     *
     * @param metadata  the metadata to update
     */
    void save(FileMetadata metadata) throws DbException;

    /**
     * Deletes the file metadata for the specified ID in the database.
     *
     * @param id    the ID the file metadata to delete
     */
    void delete(String id) throws DbException;

}
