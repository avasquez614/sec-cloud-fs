package org.avasquez.seccloudfs.dropbox.utils;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxRequestConfig;
import org.avasquez.seccloudfs.dropbox.DropboxCloudStore;
import org.avasquez.seccloudfs.dropbox.db.model.DropboxCredentials;
import org.avasquez.seccloudfs.dropbox.db.repos.DropboxCredentialsRepository;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.utils.FileUtils;
import org.springframework.beans.factory.annotation.Required;

import javax.annotation.PostConstruct;

/**
 * Utility class for easily create {@link org.avasquez.seccloudfs.dropbox.DropboxCloudStore}s from
 * predefined configuration.
 *
 * @author avasquez
 */
public class DropboxCloudStoreFactory {

    public static final String STORE_NAME_FORMAT = "dropbox://%s/%s";

    private DbxRequestConfig requestConfig;
    private long chunkedUploadThreshold;
    private DropboxCredentialsRepository credentialsRepository;
    private String credentialsId;

    private DropboxCredentials credentials;
    private DbxClient client;

    @Required
    public void setRequestConfig(DbxRequestConfig requestConfig) {
        this.requestConfig = requestConfig;
    }

    @Required
    public void setChunkedUploadThreshold(String chunkedUploadThreshold) {
        this.chunkedUploadThreshold = FileUtils.humanReadableByteSizeToByteCount(chunkedUploadThreshold);
    }

    @Required
    public void setCredentialsRepository(DropboxCredentialsRepository credentialsRepository) {
        this.credentialsRepository = credentialsRepository;
    }

    @Required
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @PostConstruct
    public void init() throws DbException {
        credentials = getCredentials();
        client = createClient();
    }

    public DropboxCloudStore createStore(String rootFolderName) {
        return new DropboxCloudStore(getStoreName(rootFolderName), client, rootFolderName, chunkedUploadThreshold);
    }

    private DbxClient createClient() {
        return new DbxClient(requestConfig, credentials.getAccessToken());
    }

    private DropboxCredentials getCredentials() throws DbException, IllegalStateException {
        DropboxCredentials credentials = credentialsRepository.find(credentialsId);
        if (credentials != null) {
            return credentials;
        } else {
            throw new IllegalStateException("No credentials found for ID " + credentialsId);
        }
    }

    private String getStoreName(String rootFolderName) {
        return String.format(STORE_NAME_FORMAT, credentials.getAccountId(), rootFolderName);
    }

}
