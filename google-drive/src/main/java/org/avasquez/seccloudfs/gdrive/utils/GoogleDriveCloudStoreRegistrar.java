package org.avasquez.seccloudfs.gdrive.utils;

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
import javax.annotation.PostConstruct;

import org.avasquez.seccloudfs.cloud.CloudStoreRegistry;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.gdrive.GoogleDriveCloudStore;
import org.avasquez.seccloudfs.gdrive.db.model.GoogleDriveCredentials;
import org.avasquez.seccloudfs.gdrive.db.repos.GoogleDriveCredentialsRepository;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.springframework.beans.factory.annotation.Required;

/**
 * Creates {@link org.avasquez.seccloudfs.cloud.CloudStore}s from the {@link org.avasquez.seccloudfs.gdrive.db.model
 * .GoogleDriveCredentials} stored in the DB, and then registers them with the {@link org.avasquez.seccloudfs.cloud
 * .CloudStoreRegistry}.
 *
 * @author avasquez
 */
public class GoogleDriveCloudStoreRegistrar {

    public static final String STORE_NAME_PREFIX = "gdrive://";

    private String clientId;
    private String clientSecret;
    private String rootFolderName;
    private GoogleDriveCredentialsRepository credentialsRepository;
    private EmbeddedCacheManager cacheManager;
    private CloudStoreRegistry registry;

    @Required
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Required
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    @Required
    public void setRootFolderName(String rootFolderName) {
        this.rootFolderName = rootFolderName;
    }

    @Required
    public void setCredentialsRepository(GoogleDriveCredentialsRepository credentialsRepository) {
        this.credentialsRepository = credentialsRepository;
    }

    @Required
    public void setCacheManager(EmbeddedCacheManager cacheManager) {
        this.cacheManager = cacheManager;
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
        Iterable<GoogleDriveCredentials> credentialsList = findCredentials();
        for (GoogleDriveCredentials credentials : credentialsList) {
            GoogleDriveCloudStore cloudStore = createStore(credentials);
            cloudStore.init();

            registry.register(cloudStore);
        }
    }

    private Iterable<GoogleDriveCredentials> findCredentials() throws IOException {
        try {
            return credentialsRepository.findAll();
        } catch (DbException e) {
            throw new IOException("Unable to retrieve all credentials from DB", e);
        }
    }

    private GoogleDriveCloudStore createStore(GoogleDriveCredentials storedCredentials) throws IOException {
        HttpTransport transport = new NetHttpTransport();
        JsonFactory jsonFactory = new JacksonFactory();
        String id = storedCredentials.getId();
        CredentialRefreshListener refreshListener = new RepositoryCredentialRefreshListener(id, credentialsRepository);

        Credential credentials = new GoogleCredential.Builder()
            .setTransport(transport)
            .setJsonFactory(jsonFactory)
            .setClientSecrets(clientId, clientSecret)
            .addRefreshListener(refreshListener)
            .build()
            .setAccessToken(storedCredentials.getAccessToken())
            .setRefreshToken(storedCredentials.getRefreshToken())
            .setExpirationTimeMilliseconds(storedCredentials.getExpirationTime());

        Drive drive = new Drive.Builder(transport, jsonFactory, credentials).build();
        String storeName = STORE_NAME_PREFIX + storedCredentials.getUsername();
        Cache<String, File> fileCache = createFileCache(storeName);

        return new GoogleDriveCloudStore(storeName, drive, fileCache, rootFolderName);
    }

    private Cache<String, File> createFileCache(String storeName) {
        cacheManager.defineConfiguration(storeName, new ConfigurationBuilder().build());

        return cacheManager.getCache(storeName);
    }

}
