package org.avasquez.seccloudfs.storage.db.repos;

import org.avasquez.seccloudfs.db.Repository;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.storage.db.model.SliceMetadata;

/**
 * DB repository for {@link org.avasquez.seccloudfs.storage.db.model.SliceMetadata}.
 *
 * @author avasquez
 */
public interface SliceMetadataRepository extends Repository<SliceMetadata> {

    /**
     * Finds all slices associated to a data ID
     *
     * @param dataId the ID of the data where the slices came from
     *
     * @return the slices
     */
    Iterable<SliceMetadata> findByDataId(String dataId) throws DbException;

}
