package org.avasquez.seccloudfs.dropbox;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxRequestConfig;

import java.io.IOException;
import javax.annotation.PostConstruct;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.cloud.CloudStoreRegistry;
import org.avasquez.seccloudfs.dropbox.db.model.DropboxCredential;
import org.avasquez.seccloudfs.dropbox.db.repos.DropboxCredentialRepository;
import org.avasquez.seccloudfs.exception.DbException;
import org.springframework.beans.factory.annotation.Required;

/**
 * Creates {@link org.avasquez.seccloudfs.cloud.CloudStore}s from the {@link org.avasquez.seccloudfs.dropbox.db.model
 * .DropboxCredential}s stored in the DB, and then registers them with the {@link org.avasquez.seccloudfs.cloud
 * .CloudStoreRegistry}.
 *
 * @author avasquez
 */
public class DropboxCloudStoreRegistrar {

    public static final String STORE_NAME_PREFIX = "dropbox://";

    private DbxRequestConfig requestConfig;
    private DropboxCredentialRepository credentialRepository;
    private CloudStoreRegistry registry;

    @Required
    public void setRequestConfig(final DbxRequestConfig requestConfig) {
        this.requestConfig = requestConfig;
    }

    @Required
    public void setCredentialRepository(final DropboxCredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
    }

    @Required
    public void setRegistry(final CloudStoreRegistry registry) {
        this.registry = registry;
    }

    /**
     * Creates {@link org.avasquez.seccloudfs.cloud.CloudStore}s from the credentials stored in the DB, and then
     * registers them with the registry.
     */
    @PostConstruct
    public void registerStores() throws IOException {
        Iterable<DropboxCredential> credentials = findCredentials();
        for (DropboxCredential credential : credentials) {
            registry.register(createStore(credential));
        }
    }

    private Iterable<DropboxCredential> findCredentials() throws IOException {
        try {
            return credentialRepository.findAll();
        } catch (DbException e) {
            throw new IOException("Unable to retrieve credentials from DB", e);
        }
    }

    private CloudStore createStore(DropboxCredential credential) {
        DbxClient client = new DbxClient(requestConfig, credential.getAccessToken());
        String storeName = STORE_NAME_PREFIX + credential.getId();

        return new DropboxCloudStore(storeName, client);
    }

}
