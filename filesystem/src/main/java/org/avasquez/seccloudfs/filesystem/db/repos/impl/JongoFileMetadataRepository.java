package org.avasquez.seccloudfs.filesystem.db.repos.impl;

import com.mongodb.MongoException;

import org.avasquez.seccloudfs.db.impl.JongoRepository;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;
import org.avasquez.seccloudfs.filesystem.db.repos.FileMetadataRepository;
import org.jongo.Jongo;

/**
 * Created by alfonsovasquez on 02/02/14.
 */
public class JongoFileMetadataRepository extends JongoRepository<FileMetadata> implements FileMetadataRepository {

    public static final String FILE_METADATA_COLLECTION_NAME = "fileMetadata";
    public static final String FILES_QUERY = "{directory: false}";
    public static final String LAST_ACCESS_TIME_ASC_SORT = "{lastAccessTime: 1}";

    public JongoFileMetadataRepository(Jongo jongo) {
        super(FILE_METADATA_COLLECTION_NAME, jongo);
    }

    @Override
    public Class<FileMetadata> getPojoClass() {
        return FileMetadata.class;
    }

    @Override
    public Iterable<FileMetadata> findFilesSortedByLastAccessTime() throws DbException {
        try {
            return collection.find(FILES_QUERY).sort(LAST_ACCESS_TIME_ASC_SORT).as(FileMetadata.class);
        } catch (MongoException e) {
            throw new DbException("[" + collection.getName() + "] Find files sorted by last access time failed", e);
        }
    }

}
