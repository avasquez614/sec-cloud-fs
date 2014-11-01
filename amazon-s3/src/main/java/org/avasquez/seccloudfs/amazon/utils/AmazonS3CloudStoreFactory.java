package org.avasquez.seccloudfs.amazon.utils;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.s3.transfer.TransferManager;
import org.avasquez.seccloudfs.amazon.AmazonS3CloudStore;
import org.avasquez.seccloudfs.utils.FileUtils;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.springframework.beans.factory.annotation.Required;

import javax.annotation.PostConstruct;

/**
 * Utility class for easily create {@link org.avasquez.seccloudfs.amazon.AmazonS3CloudStore}s from
 * predefined configuration.
 *
 * @author avasquez
 */
public class AmazonS3CloudStoreFactory {

    public static final String STORE_NAME_FORMAT = "amazonS3://%s/%s";

    private AWSCredentials credentials;
    private String accountId;
    private String maxSize;
    private long chunkedUploadThreshold;
    private EmbeddedCacheManager cacheManager;
    private int maxCacheEntries;

    private AmazonS3 s3;
    private TransferManager transferManager;

    @Required
    public void setCredentials(AWSCredentials credentials) {
        this.credentials = credentials;
    }

    @Required
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Required
    public void setMaxSize(String maxSize) {
        this.maxSize = maxSize;
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
    public void setMaxCacheEntries(int maxCacheEntries) {
        this.maxCacheEntries = maxCacheEntries;
    }

    @PostConstruct
    public void init() {
        s3 = createClient();
        transferManager = new TransferManager(s3);
    }

    public AmazonS3CloudStore createStore(String bucketName, Region region) throws Exception {
        String storeName = getStoreName(bucketName);
        AmazonS3CloudStore cloudStore = new AmazonS3CloudStore(
                storeName,
                s3,
                transferManager,
                bucketName,
                region,
                chunkedUploadThreshold,
                createMetadataCache(storeName));

        cloudStore.setMaxSize(maxSize);

        return cloudStore;
    }

    private AmazonS3 createClient() {
        return new AmazonS3Client(credentials);
    }

    private Cache<String, ObjectMetadata> createMetadataCache(String storeName) {
        Configuration conf = new ConfigurationBuilder().eviction().maxEntries(maxCacheEntries).build();

        cacheManager.defineConfiguration(storeName, conf);

        return cacheManager.getCache(storeName);
    }

    private String getStoreName(String bucketName) {
        return String.format(STORE_NAME_FORMAT, accountId, bucketName);
    }

}
