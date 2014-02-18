package org.avasquez.seccloudfs.filesystem.db.repos.impl;

import com.mongodb.MongoException;
import org.avasquez.seccloudfs.db.mongo.JongoRepository;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;
import org.avasquez.seccloudfs.filesystem.db.repos.FileMetadataRepository;
import org.jongo.Jongo;

/**
 * Created by alfonsovasquez on 02/02/14.
 */
public class JongoFileMetadataRepository extends JongoRepository<FileMetadata> implements FileMetadataRepository {

    public static final String FILE_METADATA_COLLECTION_NAME =  "fileMetadata";
    public static final String LAST_ACCESS_TIME_DESC_SORT =     "{lastAccessTime: -1}";

    public JongoFileMetadataRepository(Jongo jongo) {
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
            throw new DbException("[" + collection.getName() + "] Find all sorted by descending last access " +
                    "time failed", e);
        }
    }

}
