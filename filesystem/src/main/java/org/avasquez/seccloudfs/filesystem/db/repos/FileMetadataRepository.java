package org.avasquez.seccloudfs.filesystem.db.repos;

import org.avasquez.seccloudfs.db.Repository;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;

/**
 * DB repository for {@link org.avasquez.seccloudfs.filesystem.db.model.FileMetadata}s.
 */
public interface FileMetadataRepository extends Repository<FileMetadata> {

    /**
     * Returns the metadata of all files, sorted by descending last access time.
     *
     * @return the sorted file metadata list
     */
    Iterable<FileMetadata> findFilesSortedByLastAccessTime() throws DbException;

}
