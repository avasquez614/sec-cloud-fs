package org.avasquez.seccloudfs.amazon.utils;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Region;
import org.avasquez.seccloudfs.amazon.AmazonS3CloudStore;
import org.avasquez.seccloudfs.utils.FileUtils;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Required;

/**
 * {@link org.springframework.beans.factory.FactoryBean} that helps instance a particular {@link org.avasquez
 * .seccloudfs.amazon.AmazonS3CloudStore}.
 *
 * @author avasquez
 */
public class AmazonS3CloudStoreFactoryBean implements FactoryBean<AmazonS3CloudStore> {

    public static final String STORE_NAME_PREFIX = "amazonS3://";

    private String accessKey;
    private String secretKey;
    private String accountId;
    private String bucketName;
    private Region region;
    private String maxSize;
    private long chunkedUploadThreshold;
    private EmbeddedCacheManager cacheManager;
    private int maxCacheEntries;

    public AmazonS3CloudStoreFactoryBean() {
        region = Region.US_Standard;
    }

    @Required
    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    @Required
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @Required
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Required
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public void setRegion(Region region) {
        this.region = region;
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

    @Override
    public AmazonS3CloudStore getObject() throws Exception {
        String storeName = STORE_NAME_PREFIX + accountId;
        AmazonS3 s3 = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
        Cache<String, ObjectMetadata> metadataCache = createMetadataCache(storeName);
        AmazonS3CloudStore cloudStore = new AmazonS3CloudStore(
                storeName,
                s3,
                bucketName,
                region,
                chunkedUploadThreshold,
                metadataCache);

        cloudStore.setMaxSize(maxSize);
        cloudStore.init();

        return cloudStore;
    }

    @Override
    public Class<?> getObjectType() {
        return AmazonS3CloudStore.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    private Cache<String, ObjectMetadata> createMetadataCache(String storeName) {
        Configuration conf = new ConfigurationBuilder().eviction().maxEntries(maxCacheEntries).build();

        cacheManager.defineConfiguration(storeName, conf);

        return cacheManager.getCache(storeName);
    }

}
