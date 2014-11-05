package org.avasquez.seccloudfs.processing.db.repos.impl;

import com.mongodb.MongoException;
import org.avasquez.seccloudfs.db.impl.JongoRepository;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.processing.db.model.Upload;
import org.avasquez.seccloudfs.processing.db.repos.UploadRepository;
import org.jongo.Jongo;

import java.util.Iterator;

/**
 * Jongo repository for {@link org.avasquez.seccloudfs.processing.db.model.Upload}.
 *
 * @author avasquez
 */
public class JongoUploadRepository extends JongoRepository<Upload> implements UploadRepository {

    public static final String UPLOAD_COLLECTION_NAME = "uploads";
    public static final String FIND_LAST_SUCCESSFUL_BY_DATA_ID_QUERY = "{dataId: #, success: true}";
    public static final String FIND_BY_DATA_ID_QUERY = "{dataId: #}";
    public static final String FINISH_DATE_DESCENDING_SORT = "{finishDate: -1}";

    public JongoUploadRepository(Jongo jongo) {
        super(UPLOAD_COLLECTION_NAME, jongo);
    }

    @Override
    public Class<Upload> getPojoClass() {
        return Upload.class;
    }

    @Override
    public Upload findLastSuccessfulByDataId(final String dataId) throws DbException {
        try {
            Iterator<Upload> iter = collection.find(FIND_LAST_SUCCESSFUL_BY_DATA_ID_QUERY, dataId)
                    .sort(FINISH_DATE_DESCENDING_SORT)
                    .limit(1)
                    .as(Upload.class);

            if (iter.hasNext()) {
                return iter.next();
            } else {
                return null;
            }
        } catch (MongoException e) {
            throw new DbException("[" + collection.getName() + "] Find last successful by data ID '" + dataId +
                    "' failed", e);
        }
    }

    @Override
    public Iterable<Upload> findByDataId(String dataId) throws DbException {
        try {
            return collection.find(FIND_BY_DATA_ID_QUERY, dataId).as(Upload.class);
        } catch (MongoException e) {
            throw new DbException("[" + collection.getName() + "] Find by data ID '" + dataId + "' failed", e);
        }
    }

}
