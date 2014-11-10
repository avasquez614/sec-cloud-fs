package org.avasquez.seccloudfs.amazon.utils;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.TransferManager;

import java.io.IOException;

import org.avasquez.seccloudfs.amazon.AmazonS3CloudStore;
import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.cloud.impl.AbstractRootFolderBasedCloudStore;
import org.avasquez.seccloudfs.utils.FileUtils;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.springframework.beans.factory.annotation.Required;

/**
 * {@link org.avasquez.seccloudfs.cloud.CloudStoreRegistrar} for Amazon S3.
 */
public class AmazonS3CloudStoreRegistrar extends AbstractRootFolderBasedCloudStore<TransferManager, AmazonCredentials> {

    public static final String STORE_NAME_FORMAT = "amazon://%s/%s";

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
    protected CloudStore createStore(TransferManager transferManager, AmazonCredentials credentials,
                                     String bucketName) throws IOException {
        String storeName = String.format(STORE_NAME_FORMAT, credentials.getAccountId(), bucketName);

        return new AmazonS3CloudStore(
            storeName,
            transferManager.getAmazonS3Client(),
            transferManager,
            bucketName,
            chunkedUploadThreshold,
            createMetadataCache(storeName));
    }

    private Cache<String, ObjectMetadata> createMetadataCache(String storeName) {
        Configuration conf = new ConfigurationBuilder().eviction().maxEntries(maxEntriesPerCache).build();

        cacheManager.defineConfiguration(storeName, conf);

        return cacheManager.getCache(storeName);
    }

}
