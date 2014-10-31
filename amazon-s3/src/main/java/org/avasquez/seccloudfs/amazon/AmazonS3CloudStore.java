package org.avasquez.seccloudfs.amazon;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.TransferManager;
import org.apache.commons.io.IOUtils;
import org.avasquez.seccloudfs.cloud.impl.MaxSizeAwareCloudStore;
import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Amazon S3 implementation of {@link org.avasquez.seccloudfs.cloud.CloudStore}.
 *
 * @author avasquez
 */
public class AmazonS3CloudStore extends MaxSizeAwareCloudStore {

    private static final Logger logger = LoggerFactory.getLogger(AmazonS3CloudStore.class);

    private static final String BINARY_MIME_TYPE = "application/octet-stream";

    private String name;
    private AmazonS3 s3;
    private TransferManager transferManager;
    private String bucketName;
    private Region region;
    private long chunkedUploadThreshold;
    private Cache<String, ObjectMetadata> metadataCache;

    public AmazonS3CloudStore(String name, AmazonS3 s3, String bucketName, long chunkedUploadThreshold,
                              Cache<String, ObjectMetadata> metadataCache) {
        this.name = name;
        this.s3 = s3;
        this.transferManager = new TransferManager(s3);
        this.bucketName = bucketName;
        this.chunkedUploadThreshold = chunkedUploadThreshold;
        this.metadataCache = metadataCache;
    }

    public AmazonS3CloudStore(String name, AmazonS3 s3, String bucketName, Region region, long chunkedUploadThreshold,
                              Cache<String, ObjectMetadata> metadataCache) {
        this.name = name;
        this.s3 = s3;
        this.transferManager = new TransferManager(s3);
        this.bucketName = bucketName;
        this.region = region;
        this.chunkedUploadThreshold = chunkedUploadThreshold;
        this.metadataCache = metadataCache;
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
            bucketExists = s3.doesBucketExist(bucketName);
        } catch (Exception e) {
            throw new IOException("Error checking if bucket '" + bucketName + "' of store " + name + " exists", e);
        }

        if (!bucketExists) {
            logger.info("Bucket '" + bucketName + "' of store " + name + " does not exist. Creating it...");

            try {
                if (region != null) {
                    s3.createBucket(bucketName, region);
                } else {
                    s3.createBucket(bucketName);
                }
            } catch (Exception e) {
                throw new IOException("Error creating bucket '" + name + "' of store " + name, e);
            }
        }

        super.init();
    }

    @PreDestroy
    public void destroy() {
        transferManager.shutdownNow();
    }

    @Override
    protected Object getMetadata(String filename) throws IOException {
        logger.debug("Retrieving metadata for {}/{}/{}", name, bucketName, filename);

        ObjectMetadata metadata = metadataCache.get(filename);
        if (metadata == null) {
            try {
                metadata = s3.getObjectMetadata(bucketName, filename);
                metadataCache.put(filename, metadata);
            } catch (Exception e) {
                if (e instanceof AmazonServiceException && ((AmazonServiceException)e).getStatusCode() == 404) {
                    // Return null when not found
                    return null;
                }

                throw new IOException("Error retrieving metadata for " + name + "/" + bucketName + "/" + filename, e);
            }
        }

        return metadata;
    }

    @Override
    protected void doUpload(String filename, Object metadata, ReadableByteChannel src, long length) throws IOException {
        logger.debug("Started uploading {}/{}/{}", name, bucketName, filename);

        ObjectMetadata objectMetadata;
        if (metadata != null) {
            objectMetadata = (ObjectMetadata)metadata;
        } else {
            objectMetadata = new ObjectMetadata();
        }

        objectMetadata.setContentType(BINARY_MIME_TYPE);
        objectMetadata.setContentLength(length);

        try {
            InputStream content = Channels.newInputStream(src);

            if (length < chunkedUploadThreshold) {
                logger.debug("Using direct upload for {}/{}/{}", name, bucketName, filename);

                s3.putObject(bucketName, filename, content, objectMetadata);
            } else {
                logger.debug("Using chunked upload for {}/{}/{}", name, bucketName, filename);

                transferManager.upload(bucketName, filename, content, objectMetadata).waitForCompletion();
            }
        } catch (Exception e) {
            throw new IOException("Error uploading " + name + "/" + bucketName + "/" + filename, e);
        }

        metadataCache.put(filename, objectMetadata);

        logger.debug("Finished uploading {}/{}/{}", name, bucketName, filename);
    }

    @Override
    protected void doDownload(String filename, Object metadata, WritableByteChannel target) throws IOException {
        if (metadata == null) {
            throw new FileNotFoundException("No file " + name + "/" + bucketName + "/" + filename + " found");
        }

        logger.debug("Started downloading {}/{}/{}", name, bucketName, filename);

        try {
            S3Object s3Object = s3.getObject(bucketName, filename);

            try (InputStream in = s3Object.getObjectContent()) {
                IOUtils.copy(in, Channels.newOutputStream(target));
            }
        } catch (Exception e) {
            throw new IOException("Error downloading " + name + "/" + bucketName + "/" + filename, e);
        }

        logger.debug("Finished downloading {}/{}/{}", name, bucketName, filename);
    }

    @Override
    protected void doDelete(String filename, Object metadata) throws IOException {
        if (metadata != null) {
            logger.debug("Deleting {}/{}/{}", name, bucketName, filename);

            try {
                s3.deleteObject(bucketName, filename);
            } catch (Exception e) {
                throw new IOException("Error deleting " + name + "/" + bucketName + "/" + filename, e);
            }
        }
    }

    @Override
    protected long getDataSize(Object metadata) throws IOException {
        if (metadata != null) {
            return ((ObjectMetadata)metadata).getContentLength();
        } else {
            return 0;
        }
    }

    @Override
    protected long calculateCurrentSize() throws IOException {
        try {
            ObjectListing objectListing = s3.listObjects(new ListObjectsRequest().withBucketName(bucketName));
            long total = getTotalSizeOfObjects(objectListing);

            while (objectListing.isTruncated()) {
                objectListing = s3.listNextBatchOfObjects(objectListing);
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
