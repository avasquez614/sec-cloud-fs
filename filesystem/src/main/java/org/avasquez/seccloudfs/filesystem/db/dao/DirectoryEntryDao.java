package org.avasquez.seccloudfs.filesystem.db.dao;

import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.filesystem.db.model.DirectoryEntry;

/**
 * Created by alfonsovasquez on 01/02/14.
 */
public interface DirectoryEntryDao {

    Iterable<DirectoryEntry> findByDirectoryId(String dirId) throws DbException;

    void insert(DirectoryEntry entry) throws DbException;

    void save(DirectoryEntry entry) throws DbException;

    void delete(String id) throws DbException;

}
