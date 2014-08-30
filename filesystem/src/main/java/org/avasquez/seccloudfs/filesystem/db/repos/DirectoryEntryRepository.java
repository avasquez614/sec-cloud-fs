package org.avasquez.seccloudfs.filesystem.db.repos;

import org.avasquez.seccloudfs.db.Repository;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.filesystem.db.model.DirectoryEntry;

/**
 * Created by alfonsovasquez on 01/02/14.
 */
public interface DirectoryEntryRepository extends Repository<DirectoryEntry> {

    Iterable<DirectoryEntry> findByDirectoryId(String dirId) throws DbException;

}
