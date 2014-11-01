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
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.gdrive.GoogleDriveCloudStore;
import org.avasquez.seccloudfs.gdrive.db.model.GoogleDriveCredentials;
import org.avasquez.seccloudfs.gdrive.db.repos.GoogleDriveCredentialsRepository;
import org.avasquez.seccloudfs.utils.FileUtils;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.springframework.beans.factory.annotation.Required;

import javax.annotation.PostConstruct;
import java.io.IOException;

/**
 * Utility class for easily create {@link org.avasquez.seccloudfs.gdrive.GoogleDriveCloudStore}s from
 * predefined configuration.
 *
 * @author avasquez
 */
public class GoogleDriveCloudStoreFactory {

    public static final String STORE_NAME_FORMAT = "gdrive://%s/%s";

    private String clientId;
    private String clientSecret;
    private String applicationName;
    private EmbeddedCacheManager cacheManager;
    private int maxCacheEntries;
    private long chunkedUploadThreshold;
    private GoogleDriveCredentialsRepository credentialsRepository;
    private String credentialsId;

    private GoogleDriveCredentials credentials;
    private Drive drive;

    @Required
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Required
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    @Required
    public void setApplicationName(final String applicationName) {
        this.applicationName = applicationName;
    }

    @Required
    public void setChunkedUploadThreshold(String chunkedUploadThreshold) {
        this.chunkedUploadThreshold = FileUtils.humanReadableByteSizeToByteCount(chunkedUploadThreshold);
    }

    @Required
    public void setCacheManager(EmbeddedCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Required
    public void setMaxCacheEntries(final int maxCacheEntries) {
        this.maxCacheEntries = maxCacheEntries;
    }

    @Required
    public void setCredentialsRepository(GoogleDriveCredentialsRepository credentialsRepository) {
        this.credentialsRepository = credentialsRepository;
    }

    @Required
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @PostConstruct
    public void init() throws IOException, DbException {
        credentials = getCredentials();
        drive = createClient();
    }

    public GoogleDriveCloudStore createStore(String rootFolderName) {
        String storeName = getStoreName(rootFolderName);
        Cache<String, File> fileCache = createFileCache(storeName);

        return new GoogleDriveCloudStore(storeName, drive, rootFolderName, chunkedUploadThreshold, fileCache);
    }

    private Drive createClient() throws DbException, IllegalStateException, IOException {
        HttpTransport transport = new NetHttpTransport();
        JsonFactory jsonFactory = new JacksonFactory();
        CredentialRefreshListener refreshListener = new RepositoryCredentialRefreshListener(
                credentials,
                credentialsRepository);

        Credential cred = new GoogleCredential.Builder()
                .setTransport(transport)
                .setJsonFactory(jsonFactory)
                .setClientSecrets(clientId, clientSecret)
                .addRefreshListener(refreshListener)
                .build()
                .setAccessToken(credentials.getAccessToken())
                .setRefreshToken(credentials.getRefreshToken())
                .setExpirationTimeMilliseconds(credentials.getExpirationTime());

        return new Drive.Builder(transport, jsonFactory, cred)
                .setApplicationName(applicationName)
                .build();
    }

    private GoogleDriveCredentials getCredentials() throws DbException, IllegalStateException {
        GoogleDriveCredentials credentials = credentialsRepository.find(credentialsId);
        if (credentials != null) {
            return credentials;
        } else {
            throw new IllegalStateException("No credentials found for ID " + credentialsId);
        }
    }

    private String getStoreName(String rootFolderName) {
        return String.format(STORE_NAME_FORMAT, credentials.getAccountId(), rootFolderName);
    }

    private Cache<String, File> createFileCache(String storeName) {
        Configuration conf = new ConfigurationBuilder().eviction().maxEntries(maxCacheEntries).build();

        cacheManager.defineConfiguration(storeName, conf);

        return cacheManager.getCache(storeName);
    }

}
