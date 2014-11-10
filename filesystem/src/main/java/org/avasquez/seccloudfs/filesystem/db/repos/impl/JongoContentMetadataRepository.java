package org.avasquez.seccloudfs.filesystem.db.repos.impl;

import com.mongodb.MongoException;

import org.avasquez.seccloudfs.db.impl.JongoRepository;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.filesystem.db.model.ContentMetadata;
import org.avasquez.seccloudfs.filesystem.db.repos.ContentMetadataRepository;
import org.jongo.Jongo;

/**
 * Created by alfonsovasquez on 02/02/14.
 */
public class JongoContentMetadataRepository extends JongoRepository<ContentMetadata> implements ContentMetadataRepository {

    public static final String CONTENT_METADATA_COLLECTION_NAME = "contentMetadata";
    public static final String FIND_MARKED_AS_DELETED_QUERY = "{markedAsDeleted: true}";

    public JongoContentMetadataRepository(Jongo jongo) {
        super(CONTENT_METADATA_COLLECTION_NAME, jongo);
    }

    @Override
    public Class<ContentMetadata> getPojoClass() {
        return ContentMetadata.class;
    }

    @Override
    public Iterable<ContentMetadata> findMarkedAsDelete() throws DbException {
        try {
            return collection.find(FIND_MARKED_AS_DELETED_QUERY).as(ContentMetadata.class);
        } catch (MongoException e) {
            throw new DbException("[" + collection.getName() + "] Find marked as deleted failed", e);
        }
    }

}
