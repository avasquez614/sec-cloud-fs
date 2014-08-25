package org.avasquez.seccloudfs.gdrive.db.repos;

import org.avasquez.seccloudfs.db.Repository;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.gdrive.db.model.GoogleDriveCredential;

/**
 * Repository for {@link org.avasquez.seccloudfs.gdrive.db.model.GoogleDriveCredential}s.
 *
 * @author avasquez
 */
public interface GoogleDriveCredentialRepository extends Repository<GoogleDriveCredential> {

    /**
     * Finds a credential for a specified user.
     *
     * @param userId the ID of the user
     *
     * @return the credential, or null if not found
     */
    GoogleDriveCredential findByUserId(String userId) throws DbException;

}
