package org.avasquez.seccloudfs.amazon.utils;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import org.avasquez.seccloudfs.amazon.AmazonS3CloudStore;
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
    private String username;
    private String bucketName;

    @Required
    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    @Required
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @Required
    public void setUsername(String username) {
        this.username = username;
    }

    @Required
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    @Override
    public AmazonS3CloudStore getObject() throws Exception {
        String storeName = STORE_NAME_PREFIX + username;
        AmazonS3 s3 = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
        AmazonS3CloudStore cloudStore = new AmazonS3CloudStore(storeName, s3, bucketName);

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

}
