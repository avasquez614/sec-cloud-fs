package org.avasquez.seccloudfs.gdrive;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialRefreshListener;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.io.IOException;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.cloud.CloudStoreRegistrar;
import org.avasquez.seccloudfs.cloud.CloudStoreRegistry;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.gdrive.db.model.GoogleDriveCredential;
import org.avasquez.seccloudfs.gdrive.db.repos.GoogleDriveCredentialRepository;
import org.avasquez.seccloudfs.gdrive.utils.RepositoryCredentialRefreshListener;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.springframework.beans.factory.annotation.Required;

/**
 * Creates {@link org.avasquez.seccloudfs.cloud.CloudStore}s from the {@link org.avasquez.seccloudfs.gdrive.db.model
 * .GoogleDriveCredential}s stored in the DB, and then registers them with the {@link org.avasquez.seccloudfs.cloud
 * .CloudStoreRegistry}.
 *
 * @author avasquez
 */
public class GoogleDriveCloudStoreRegistrar implements CloudStoreRegistrar {

    public static final String STORE_NAME_PREFIX = "gdrive://";

    private String clientId;
    private String clientSecret;
    private GoogleDriveCredentialRepository credentialRepository;
    private String rootFolderName;
    private EmbeddedCacheManager cacheManager;

    @Required
    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    @Required
    public void setClientSecret(final String clientSecret) {
        this.clientSecret = clientSecret;
    }

    @Required
    public void setCredentialRepository(final GoogleDriveCredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
    }

    @Required
    public void setRootFolderName(final String rootFolderName) {
        this.rootFolderName = rootFolderName;
    }

    @Required
    public void setCacheManager(final EmbeddedCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Creates {@link org.avasquez.seccloudfs.cloud.CloudStore}s from the credentials stored in the DB, and then
     * registers them with the specified registry
     *
     * @param registry the registry to register the stores to
     */
    @Override
    public void registerStores(final CloudStoreRegistry registry) throws IOException {
        Iterable<GoogleDriveCredential> credentials = findCredentials();
        for (GoogleDriveCredential credential : credentials) {
            registry.register(createStore(credential));
        }
    }

    private Iterable<GoogleDriveCredential> findCredentials() throws IOException {
        try {
            return credentialRepository.findAll();
        } catch (DbException e) {
            throw new IOException("Unable to retrieve credentials from DB", e);
        }
    }

    private CloudStore createStore(GoogleDriveCredential storedCredential) {
        HttpTransport transport = new NetHttpTransport();
        JsonFactory jsonFactory = new JacksonFactory();
        String id = storedCredential.getId();
        CredentialRefreshListener refreshListener = new RepositoryCredentialRefreshListener(id, credentialRepository);

        Credential credential = new GoogleCredential.Builder()
            .setTransport(transport)
            .setJsonFactory(jsonFactory)
            .setClientSecrets(clientId, clientSecret)
            .addRefreshListener(refreshListener)
            .build()
            .setAccessToken(storedCredential.getAccessToken())
            .setRefreshToken(storedCredential.getRefreshToken())
            .setExpirationTimeMilliseconds(storedCredential.getExpirationTime());

        Drive drive = new Drive.Builder(transport, jsonFactory, credential).build();
        String storeName = STORE_NAME_PREFIX + id;
        Cache<String, File> fileCache = createFileCache(storeName);

        return new GoogleDriveCloudStore(storeName, drive, fileCache, rootFolderName);
    }

    private Cache<String, File> createFileCache(String storeName) {
        cacheManager.defineConfiguration(storeName, new ConfigurationBuilder().build());

        return cacheManager.getCache(storeName);
    }

}
