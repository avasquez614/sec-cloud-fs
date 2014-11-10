package org.avasquez.seccloudfs.gdrive.utils;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.io.IOException;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.cloud.impl.AbstractRootFolderBasedCloudStore;
import org.avasquez.seccloudfs.gdrive.GoogleDriveCloudStore;
import org.avasquez.seccloudfs.gdrive.db.model.GoogleDriveCredentials;
import org.avasquez.seccloudfs.utils.FileUtils;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.springframework.beans.factory.annotation.Required;

/**
 * {@link org.avasquez.seccloudfs.cloud.CloudStoreRegistrar} for Google Drive.
 *
 * @author avasquez
 */
public class GoogleDriveCloudStoreRegistrar extends AbstractRootFolderBasedCloudStore<Drive, GoogleDriveCredentials> {

    public static final String STORE_NAME_FORMAT = "gdrive://%s/%s";

    private long chunkedUploadThreshold;
    private EmbeddedCacheManager cacheManager;
    private int maxEntriesPerCache;

    @Required
    public void setChunkedUploadThreshold(String chunkedUploadThreshold) {
        this.chunkedUploadThreshold = FileUtils.humanReadableByteSizeToByteCount(chunkedUploadThreshold);
    }

    @Required
    public void setCacheManager(EmbeddedCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Required
    public void setMaxEntriesPerCache(int maxEntriesPerCache) {
        this.maxEntriesPerCache = maxEntriesPerCache;
    }

    @Override
    protected CloudStore createStore(Drive client, GoogleDriveCredentials credentials,
                                     String rootFolderName) throws IOException {
        String storeName = String.format(STORE_NAME_FORMAT, credentials.getAccountId(), rootFolderName);
        Cache<String, File> cache = createFileCache(storeName);

        return new GoogleDriveCloudStore(storeName, client, rootFolderName, chunkedUploadThreshold, cache);
    }

    private Cache<String, File> createFileCache(String storeName) {
        Configuration conf = new ConfigurationBuilder().eviction().maxEntries(maxEntriesPerCache).build();

        cacheManager.defineConfiguration(storeName, conf);

        return cacheManager.getCache(storeName);
    }

}
