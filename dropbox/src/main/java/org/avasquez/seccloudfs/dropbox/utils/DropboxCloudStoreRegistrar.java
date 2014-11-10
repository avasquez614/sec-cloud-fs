package org.avasquez.seccloudfs.dropbox.utils;

import com.dropbox.core.DbxClient;

import java.io.IOException;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.cloud.impl.AbstractRootFolderBasedCloudStore;
import org.avasquez.seccloudfs.dropbox.DropboxCloudStore;
import org.avasquez.seccloudfs.dropbox.db.model.DropboxCredentials;
import org.avasquez.seccloudfs.utils.FileUtils;
import org.springframework.beans.factory.annotation.Required;

/**
 * {@link org.avasquez.seccloudfs.cloud.CloudStoreRegistrar} for Dropbox.
 *
 * @author avasquez
 */
public class DropboxCloudStoreRegistrar extends AbstractRootFolderBasedCloudStore<DbxClient, DropboxCredentials> {

    private static final String STORE_NAME_FORMAT = "dropbox://%s/%s";

    private long chunkedUploadThreshold;

    @Required
    public void setChunkedUploadThreshold(String chunkedUploadThreshold) {
        this.chunkedUploadThreshold = FileUtils.humanReadableByteSizeToByteCount(chunkedUploadThreshold);
    }

    @Override
    protected CloudStore createStore(DbxClient client, DropboxCredentials credentials,
                                     String rootFolderName) throws IOException {
        String storeName = String.format(STORE_NAME_FORMAT, credentials.getAccountId(), rootFolderName);

        return new DropboxCloudStore(storeName, client, rootFolderName, chunkedUploadThreshold);
    }

}
