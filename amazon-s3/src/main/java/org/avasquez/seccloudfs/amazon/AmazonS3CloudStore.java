package org.avasquez.seccloudfs.amazon;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.transfer.TransferManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.io.IOUtils;
import org.avasquez.seccloudfs.cloud.CloudStore;
import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Amazon S3 implementation of {@link org.avasquez.seccloudfs.cloud.CloudStore}. The implementation is synchronized
 * to avoid too many open connections (while testing, a lot of HTTP connections failed in the server side).
 *
 * @author avasquez
 */
public class AmazonS3CloudStore implements CloudStore {

    private static final Logger logger = LoggerFactory.getLogger(AmazonS3CloudStore.class);

    private static final String BINARY_MIME_TYPE = "application/octet-stream";

    private String name;
    private AmazonS3 s3;
    private TransferManager transferManager;
    private String bucketName;
    private long chunkedUploadThreshold;
    private Cache<String, ObjectMetadata> metadataCache;

    public AmazonS3CloudStore(String name, AmazonS3 s3, TransferManager transferManager, String bucketName,
                              long chunkedUploadThreshold, Cache<String, ObjectMetadata> metadataCache) {
        this.name = name;
        this.s3 = s3;
        this.transferManager = transferManager;
        this.bucketName = bucketName;
        this.chunkedUploadThreshold = chunkedUploadThreshold;
        this.metadataCache = metadataCache;
    }

    @PostConstruct
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
                s3.createBucket(bucketName);
            } catch (Exception e) {
                throw new IOException("Error creating bucket '" + name + "' of store " + name, e);
            }
        }
    }

    @PreDestroy
    public void destroy() {
        transferManager.shutdownNow();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public synchronized void upload(String id, ReadableByteChannel src, long length) throws IOException {
        logger.debug("Started uploading {}/{}", name, id);

        ObjectMetadata metadata = getMetadata(id);
        if (metadata == null) {
            metadata = new ObjectMetadata();
        }

        metadata.setContentType(BINARY_MIME_TYPE);
        metadata.setContentLength(length);

        try {
            InputStream content = Channels.newInputStream(src);

            if (length < chunkedUploadThreshold) {
                logger.debug("Using direct upload for {}/{}", name, id);

                s3.putObject(bucketName, id, content, metadata);
            } else {
                logger.debug("Using chunked upload for {}/{}", name, id);

                transferManager.upload(bucketName, id, content, metadata).waitForCompletion();
            }
        } catch (Exception e) {
            throw new IOException("Error uploading " + name + "/" + id, e);
        }

        metadataCache.put(id, metadata);

        logger.debug("Finished uploading {}/{}", name, id);
    }

    @Override
    public synchronized void download(String id, WritableByteChannel target) throws IOException {
        ObjectMetadata metadata = getMetadata(id);
        if (metadata == null) {
            throw new FileNotFoundException("No file " + name + "/" + id + " found");
        }

        logger.debug("Started downloading {}/{}", name, id);

        try {
            S3Object s3Object = s3.getObject(bucketName, id);

            try (InputStream in = s3Object.getObjectContent()) {
                IOUtils.copy(in, Channels.newOutputStream(target));
            }
        } catch (Exception e) {
            throw new IOException("Error downloading " + name + "/" + id, e);
        }

        logger.debug("Finished downloading {}/{}", name, id);
    }

    @Override
    public synchronized void delete(String id) throws IOException {
        ObjectMetadata metadata = getMetadata(id);
        if (metadata != null) {
            logger.debug("Deleting {}/{}", name, id);

            try {
                s3.deleteObject(bucketName, id);
            } catch (Exception e) {
                throw new IOException("Error deleting " + name + "/" + id, e);
            }
        }
    }

    private ObjectMetadata getMetadata(String filename) throws IOException {
        logger.debug("Retrieving metadata for {}/{}", name, filename);

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

                throw new IOException("Error retrieving metadata for " + name + "/" + filename, e);
            }
        }

        return metadata;
    }

}
