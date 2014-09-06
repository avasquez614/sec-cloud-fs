package org.avasquez.seccloudfs.gdrive.db.repos.impl;

import org.avasquez.seccloudfs.db.mongo.JongoRepository;
import org.avasquez.seccloudfs.gdrive.db.model.GoogleDriveCredentials;
import org.avasquez.seccloudfs.gdrive.db.repos.GoogleDriveCredentialsRepository;
import org.jongo.Jongo;

/**
 * Jongo repository for {@link org.avasquez.seccloudfs.gdrive.db.model.GoogleDriveCredentials}.
 *
 * @author avasquez
 */
public class JongoGoogleDriveCredentialsRepository extends JongoRepository<GoogleDriveCredentials>
    implements GoogleDriveCredentialsRepository {

    public static final String GOOGLE_DRIVE_CREDENTIALS_COLLECTION_NAME = "googleDriveCredentials";

    protected JongoGoogleDriveCredentialsRepository(final Jongo jongo) {
        super(GOOGLE_DRIVE_CREDENTIALS_COLLECTION_NAME, jongo);
    }

    @Override
    public Class<GoogleDriveCredentials> getPojoClass() {
        return GoogleDriveCredentials.class;
    }

}
