package org.avasquez.seccloudfs.gdrive.db.repos.impl;

import com.mongodb.MongoException;

import org.avasquez.seccloudfs.db.mongo.JongoRepository;
import org.avasquez.seccloudfs.exception.DbException;
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
    public static final String GOOGLE_DRIVE_CREDENTIALS_COLLECTION_INDEX_KEYS = "{userId: 1}";
    public static final String FIND_BY_USER_ID_QUERY = "{userId: #}";

    protected JongoGoogleDriveCredentialRepository(final Jongo jongo) {
        super(GOOGLE_DRIVE_CREDENTIALS_COLLECTION_NAME, jongo);

        collection.ensureIndex(GOOGLE_DRIVE_CREDENTIALS_COLLECTION_INDEX_KEYS);
    }

    @Override
    public GoogleDriveCredential findByUserId(final String userId) throws DbException {
        try {
            return collection.findOne(FIND_BY_USER_ID_QUERY, userId).as(GoogleDriveCredential.class);
        } catch (MongoException e) {
            throw new DbException("[" + collection.getName() + "] Find by user ID '" + userId + "' failed", e);
        }
    }

    @Override
    public Class<GoogleDriveCredential> getPojoClass() {
        return GoogleDriveCredential.class;
    }

}
