package org.avasquez.seccloudfs.filesystem.db.dao;

import org.avasquez.seccloudfs.filesystem.db.model.DirectoryEntry;

/**
 * Created by alfonsovasquez on 01/02/14.
 */
public interface DirectoryEntryDao {

    DirectoryEntry find(String id);

    Iterable<DirectoryEntry> findByDirectoryId(String dirId);

    void insert(DirectoryEntry metadata);

    void save(DirectoryEntry metadata);

    void delete(String id);

}
