package org.avasquez.seccloudfs.processing.db.repos;

import org.avasquez.seccloudfs.db.Repository;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.processing.db.model.Upload;

/**
 * DB repository for {@link org.avasquez.seccloudfs.processing.db.model.Upload}.
 *
 * @author avasquez
 */
public interface UploadRepository extends Repository<Upload> {

    /**
     * Finds the last successful upload associated to the data ID
     *
     * @param dataId the ID of the data
     *
     * @return the upload
     */
    Upload findLastSuccessfulByDataId(String dataId) throws DbException;

    /**
     * Finds all uploads associated to the data ID
     *
     * @param dataId the ID of the data
     *
     * @return the uploads
     */
    Iterable<Upload> findByDataId(String dataId) throws DbException;

}
