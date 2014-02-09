package org.avasquez.seccloudfs.filesystem.db.dao.impl;

import com.mongodb.MongoException;
import org.avasquez.seccloudfs.db.mongo.JongoDao;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.filesystem.db.dao.FileMetadataDao;
import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;
import org.jongo.Jongo;

/**
 * Created by alfonsovasquez on 02/02/14.
 */
public class JongoFileMetadataDao extends JongoDao<FileMetadata> implements FileMetadataDao {

    public static final String FILE_METADATA_COLLECTION_NAME =  "fileMetadata";
    public static final String LAST_ACCESS_TIME_DESC_SORT =     "{lastAccessTime: -1}";

    public JongoFileMetadataDao(Jongo jongo) {
        super(FILE_METADATA_COLLECTION_NAME, jongo);
    }

    @Override
    public Class<? extends FileMetadata> getPojoClass() {
        return FileMetadata.class;
    }

    @Override
    public Iterable<FileMetadata> findAllSortedByDescLastAccessTime() throws DbException {
        try {
            return collection.find().sort(LAST_ACCESS_TIME_DESC_SORT).as(FileMetadata.class);
        } catch (MongoException e) {
            throw new DbException("Find all sorted by descending last access time failed", e);
        }
    }

}
