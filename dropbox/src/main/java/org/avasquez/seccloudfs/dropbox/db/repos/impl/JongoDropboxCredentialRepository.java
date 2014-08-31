package org.avasquez.seccloudfs.dropbox.db.repos.impl;

import org.avasquez.seccloudfs.db.mongo.JongoRepository;
import org.avasquez.seccloudfs.dropbox.db.model.DropboxCredential;
import org.avasquez.seccloudfs.dropbox.db.repos.DropboxCredentialRepository;
import org.jongo.Jongo;

/**
 * Jongo repository for {@link org.avasquez.seccloudfs.dropbox.db.model.DropboxCredential}.
 *
 * @author avasquez
 */
public class JongoDropboxCredentialRepository extends JongoRepository<DropboxCredential>
    implements DropboxCredentialRepository {

    public static final String DROPBOX_CREDENTIALS_COLLECTION_NAME = "dropboxCredentials";

    protected JongoDropboxCredentialRepository(Jongo jongo) {
        super(DROPBOX_CREDENTIALS_COLLECTION_NAME, jongo);
    }

    @Override
    public Class<DropboxCredential> getPojoClass() {
        return DropboxCredential.class;
    }
}
