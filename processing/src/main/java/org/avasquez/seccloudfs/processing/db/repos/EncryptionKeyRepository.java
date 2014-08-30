package org.avasquez.seccloudfs.processing.db.repos;

import org.avasquez.seccloudfs.db.Repository;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.processing.db.model.EncryptionKey;

/**
 * DB repository for {@link org.avasquez.seccloudfs.processing.db.model.EncryptionKey}s.
 *
 * @author avasquez
 */
public interface EncryptionKeyRepository extends Repository<EncryptionKey> {

    /**
     * Finds the encryption key associated to a data ID
     *
     * @param dataId the ID of the encrypted data
     *
     * @return the encryption key
     */
    EncryptionKey findByDataId(String dataId) throws DbException;

}
