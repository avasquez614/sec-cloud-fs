package org.avasquez.seccloudfs.processing.db.repos.impl;

import com.mongodb.MongoException;

import org.avasquez.seccloudfs.db.mongo.JongoRepository;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.processing.db.model.ErasureInfo;
import org.avasquez.seccloudfs.processing.db.repos.ErasureInfoRepository;
import org.jongo.Jongo;

/**
 * Jongo repository for {@link org.avasquez.seccloudfs.processing.db.model.SliceMetadata}.
 *
 * @author avasquez
 */
public class JongoErasureInfoRepository extends JongoRepository<ErasureInfo> implements ErasureInfoRepository {

    public static final String ERASURE_INFO_COLLECTION_NAME = "erasureInfo";
    public static final String FIND_BY_DATA_ID_QUERY = "{dataId: #}";

    public JongoErasureInfoRepository(Jongo jongo) {
        super(ERASURE_INFO_COLLECTION_NAME, jongo);
    }

    @Override
    public Class<ErasureInfo> getPojoClass() {
        return ErasureInfo.class;
    }

    @Override
    public ErasureInfo findByDataId(final String dataId) throws DbException {
        try {
            return collection.findOne(FIND_BY_DATA_ID_QUERY, dataId).as(ErasureInfo.class);
        } catch (MongoException e) {
            throw new DbException("[" + collection.getName() + "] Find by data ID '" + dataId + "' failed", e);
        }
    }

}
