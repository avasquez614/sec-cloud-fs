package org.avasquez.seccloudfs.dropbox.utils;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxRequestConfig;

import java.io.IOException;
import javax.annotation.PostConstruct;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.cloud.CloudStoreRegistry;
import org.avasquez.seccloudfs.dropbox.DropboxCloudStore;
import org.avasquez.seccloudfs.dropbox.db.model.DropboxCredentials;
import org.avasquez.seccloudfs.dropbox.db.repos.DropboxCredentialsRepository;
import org.avasquez.seccloudfs.exception.DbException;
import org.springframework.beans.factory.annotation.Required;

/**
 * Creates {@link org.avasquez.seccloudfs.cloud.CloudStore}s from the {@link org.avasquez.seccloudfs.dropbox.db.model
 * .DropboxCredentials} stored in the DB, and then registers them with the {@link org.avasquez.seccloudfs.cloud
 * .CloudStoreRegistry}.
 *
 * @author avasquez
 */
public class DropboxCloudStoreRegistrar {

    public static final String STORE_NAME_PREFIX = "dropbox://";

    private DbxRequestConfig requestConfig;
    private DropboxCredentialsRepository credentialsRepository;
    private CloudStoreRegistry registry;

    @Required
    public void setRequestConfig(DbxRequestConfig requestConfig) {
        this.requestConfig = requestConfig;
    }

    @Required
    public void setCredentialsRepository(DropboxCredentialsRepository credentialsRepository) {
        this.credentialsRepository = credentialsRepository;
    }

    @Required
    public void setRegistry(CloudStoreRegistry registry) {
        this.registry = registry;
    }

    /**
     * Creates {@link org.avasquez.seccloudfs.cloud.CloudStore}s from the credentials stored in the DB, and then
     * registers them with the registry.
     */
    @PostConstruct
    public void registerStores() throws IOException {
        Iterable<DropboxCredentials> credentialsList = findCredentials();
        for (DropboxCredentials credentials : credentialsList) {
            registry.register(createStore(credentials));
        }
    }

    private Iterable<DropboxCredentials> findCredentials() throws IOException {
        try {
            return credentialsRepository.findAll();
        } catch (DbException e) {
            throw new IOException("Unable to retrieve all credentials from DB", e);
        }
    }

    private CloudStore createStore(DropboxCredentials credentials) throws IOException {
        DbxClient client = new DbxClient(requestConfig, credentials.getAccessToken());
        String storeName = STORE_NAME_PREFIX + credentials.getUsername();

        return new DropboxCloudStore(storeName, client);
    }

}