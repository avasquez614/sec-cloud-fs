package org.avasquez.seccloudfs.amazon;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;

import org.apache.commons.io.IOUtils;
import org.avasquez.seccloudfs.cloud.impl.MaxSizeAwareCloudStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Amazon S3 implementation of {@link org.avasquez.seccloudfs.cloud.CloudStore}.
 *
 * @author avasquez
 */
public class AmazonS3CloudStore extends MaxSizeAwareCloudStore {

    private static final Logger logger = LoggerFactory.getLogger(AmazonS3CloudStore.class);

    private static final String BINARY_MIME_TYPE = "application/octet-stream";

    private String name;
    private AmazonS3 client;
    private String bucketName;

    public AmazonS3CloudStore(String name, AmazonS3 client, String bucketName) {
        this.name = name;
        this.client = client;
        this.bucketName = bucketName;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void init() throws IOException {
        // Check if bucket exists, if not create it
        boolean bucketExists;
        try {
            bucketExists = client.doesBucketExist(bucketName);
        } catch (Exception e) {
            throw new IOException("Error checking if bucket '" + bucketName + "' of store " + name + " exists", e);
        }

        if (!bucketExists) {
            logger.info("Bucket '" + bucketName + "' of store " + name + " does not exist. Creating it...");

            try {
                client.createBucket(bucketName);
            } catch (Exception e) {
                throw new IOException("Error creating bucket '" + name + "' of store " + name, e);
            }
        }

        super.init();
    }

    @Override
    protected Object getDataObject(String id, boolean withData) throws IOException {
        logger.debug("Retrieving data object '{}' from store {}", id, name);

        try {
            if (withData) {
                return client.getObject(bucketName, id);
            } else {
                return client.getObjectMetadata(bucketName, id);
            }
        } catch (Exception e) {
            throw new IOException("Error retrieving data object '" + id + "' from store " + name, e);
        }
    }

    @Override
    protected long doUpload(String id, Object dataObject, SeekableByteChannel src, long length) throws IOException {
        logger.debug("Uploading data '{}' to store {}", id, name);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(BINARY_MIME_TYPE);
        metadata.setContentLength(length);

        try {
            client.putObject(bucketName, id, Channels.newInputStream(src), metadata);
        } catch (Exception e) {
            throw new IOException("Error uploading data '" + id + "' to store " + name, e);
        }

        return length;
    }

    @Override
    protected long doDownload(String id, Object dataObject, SeekableByteChannel target) throws IOException {
        logger.debug("Downloading data '{}' from store {}", id, name);

        try {
            S3Object s3Object = (S3Object) dataObject;

            try (InputStream in = s3Object.getObjectContent()) {
                return IOUtils.copy(in, Channels.newOutputStream(target));
            }
        } catch (Exception e) {
            throw new IOException("Error downloading data '" + id + "' from store " + name, e);
        }
    }

    @Override
    protected void doDelete(String id, Object dataObject) throws IOException {
        logger.debug("Deleting data '{}' from store {}", id, name);

        try {
            client.deleteObject(bucketName, id);
        } catch (Exception e) {
            throw new IOException("Error deleting data '" + id + "' from store " + name, e);
        }
    }

    @Override
    protected long getDataSize(Object dataObject) throws IOException {
        if (dataObject instanceof S3Object) {
            return ((S3Object) dataObject).getObjectMetadata().getContentLength();
        } else {
            return ((ObjectMetadata) dataObject).getContentLength();
        }
    }

    @Override
    protected long calculateCurrentSize() throws IOException {
        try {
            ObjectListing objectListing = client.listObjects(new ListObjectsRequest().withBucketName(bucketName));
            long total = getTotalSizeOfObjects(objectListing);

            while (objectListing.isTruncated()) {
                objectListing = client.listNextBatchOfObjects(objectListing);
                total += getTotalSizeOfObjects(objectListing);
            }

            return total;
        } catch (Exception e) {
            throw new IOException("Unable to calculate size of bucket '" + bucketName + "' of store " + name, e);
        }
    }

    private long getTotalSizeOfObjects(ObjectListing objectListing) {
        long total = 0;

        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
            total += objectSummary.getSize();
        }

        return total;
    }

}
