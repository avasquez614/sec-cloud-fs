package org.avasquez.seccloudfs.gdrive.db.repos.impl;

import org.avasquez.seccloudfs.db.mongo.JongoRepository;
import org.avasquez.seccloudfs.gdrive.db.model.GoogleDriveCredential;
import org.avasquez.seccloudfs.gdrive.db.repos.GoogleDriveCredentialRepository;
import org.jongo.Jongo;

/**
 * Jongo repository for {@link org.avasquez.seccloudfs.gdrive.db.model.GoogleDriveCredential}.
 *
 * @author avasquez
 */
public class JongoGoogleDriveCredentialRepository extends JongoRepository<GoogleDriveCredential>
    implements GoogleDriveCredentialRepository {

    public static final String GOOGLE_DRIVE_CREDENTIALS_COLLECTION_NAME = "googleDriveCredentials";

    protected JongoGoogleDriveCredentialRepository(final Jongo jongo) {
        super(GOOGLE_DRIVE_CREDENTIALS_COLLECTION_NAME, jongo);
    }

    @Override
    public Class<GoogleDriveCredential> getPojoClass() {
        return GoogleDriveCredential.class;
    }

}
