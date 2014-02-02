package org.avasquez.seccloudfs.filesystem.db.dao.impl;

import com.mongodb.MongoException;
import org.avasquez.seccloudfs.db.mongo.JongoDao;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.filesystem.db.dao.DirectoryEntryDao;
import org.avasquez.seccloudfs.filesystem.db.model.DirectoryEntry;
import org.jongo.Jongo;

/**
 * Created by alfonsovasquez on 02/02/14.
 */
public class JongoDirectoryEntryDao extends JongoDao<DirectoryEntry> implements DirectoryEntryDao {

    public static final String DIRECTORY_ENTRIES_COLLECTION_NAME =  "directoryEntries";
    public static final String FIND_BY_DIRECTORY_ID_QUERY =         "{directoryId: #}";

    public JongoDirectoryEntryDao(Jongo jongo) {
        super(DIRECTORY_ENTRIES_COLLECTION_NAME, jongo);
    }

    @Override
    public Iterable<DirectoryEntry> findByDirectoryId(String dirId) throws DbException {
        try {
            return collection.find(FIND_BY_DIRECTORY_ID_QUERY, dirId).as(DirectoryEntry.class);
        } catch (MongoException e) {
            throw new DbException("Find entries for dir ID '" + dirId + "' failed", e);
        }
    }

    @Override
    public Class<? extends DirectoryEntry> getPojoClass() {
        return DirectoryEntry.class;
    }

}
