package org.avasquez.seccloudfs.dropbox.db.repos.impl;

import org.avasquez.seccloudfs.db.impl.JongoRepository;
import org.avasquez.seccloudfs.dropbox.db.model.DropboxCredentials;
import org.avasquez.seccloudfs.dropbox.db.repos.DropboxCredentialsRepository;
import org.jongo.Jongo;

/**
 * Jongo repository for {@link org.avasquez.seccloudfs.dropbox.db.model.DropboxCredentials}.
 *
 * @author avasquez
 */
public class JongoDropboxCredentialsRepository extends JongoRepository<DropboxCredentials>
    implements DropboxCredentialsRepository {

    public static final String DROPBOX_CREDENTIALS_COLLECTION_NAME = "dropboxCredentials";

    protected JongoDropboxCredentialsRepository(Jongo jongo) {
        super(DROPBOX_CREDENTIALS_COLLECTION_NAME, jongo);
    }

    @Override
    public Class<DropboxCredentials> getPojoClass() {
        return DropboxCredentials.class;
    }
}
