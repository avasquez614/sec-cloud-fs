package org.avasquez.seccloudfs.storage.db.repos.impl;

import com.mongodb.MongoException;

import org.avasquez.seccloudfs.db.mongo.JongoRepository;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.storage.db.model.SliceMetadata;
import org.avasquez.seccloudfs.storage.db.repos.SliceMetadataRepository;
import org.jongo.Jongo;

/**
 * Jongo repository for {@link org.avasquez.seccloudfs.storage.db.model.SliceMetadata}.
 *
 * @author avasquez
 */
public class JongoSliceMetadataRepository extends JongoRepository<SliceMetadata> implements SliceMetadataRepository {

    public static final String SLICE_METADATA_COLLECTION_NAME = "contentMetadata";
    public static final String FIND_BY_DATA_ID_QUERY = "{dataId: #}";

    public JongoSliceMetadataRepository(Jongo jongo) {
        super(SLICE_METADATA_COLLECTION_NAME, jongo);
    }

    @Override
    public Class<? extends SliceMetadata> getPojoClass() {
        return SliceMetadata.class;
    }

    @Override
    public Iterable<SliceMetadata> findByDataId(final String dataId) throws DbException {
        try {
            return collection.find(FIND_BY_DATA_ID_QUERY, dataId).as(SliceMetadata.class);
        } catch (MongoException e) {
            throw new DbException("[" + collection.getName() + "] Find slices for data ID '" + dataId + "' failed", e);
        }
    }

}
