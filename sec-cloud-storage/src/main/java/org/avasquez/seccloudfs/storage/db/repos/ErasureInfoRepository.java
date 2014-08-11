package org.avasquez.seccloudfs.storage.db.repos;

import org.avasquez.seccloudfs.db.Repository;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.storage.db.model.ErasureInfo;

/**
 * DB repository for {@link org.avasquez.seccloudfs.storage.db.model.ErasureInfo}.
 *
 * @author avasquez
 */
public interface ErasureInfoRepository extends Repository<ErasureInfo> {

    /**
     * Finds the erasure info associated to a data ID
     *
     * @param dataId the ID of the data
     *
     * @return the erasure info
     */
    ErasureInfo findByDataId(String dataId) throws DbException;

}
