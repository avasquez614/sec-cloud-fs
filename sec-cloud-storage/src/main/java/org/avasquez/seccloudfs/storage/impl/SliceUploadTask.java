package org.avasquez.seccloudfs.storage.impl;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.Callable;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.storage.db.model.SliceMetadata;
import org.avasquez.seccloudfs.storage.db.repos.SliceMetadataRepository;
import org.avasquez.seccloudfs.utils.nio.ByteBufferChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Asynchronous task, implemented as a {@link java.util.concurrent.Callable}, to upload a slice to a
 * {@link org.avasquez.seccloudfs.cloud.CloudStore}.
 *
 * @author avasquez
 */
public class SliceUploadTask implements Callable<Long> {

    private static final Logger logger = LoggerFactory.getLogger(SliceUploadTask.class);

    private ByteBuffer slice;
    private SliceMetadata sliceMetadata;
    private SliceMetadataRepository sliceMetadataRepository;
    private Queue<CloudStore> availableCloudStores;

    public SliceUploadTask(final ByteBuffer slice, final SliceMetadata sliceMetadata,
                           final SliceMetadataRepository sliceMetadataRepository,
                           final Queue<CloudStore> availableCloudStores) {
        this.slice = slice;
        this.sliceMetadata = sliceMetadata;
        this.sliceMetadataRepository = sliceMetadataRepository;
        this.availableCloudStores = availableCloudStores;
    }

    @Override
    public Long call() throws Exception {
        boolean uploaded = false;
        long bytesUploaded = 0;

        while (!uploaded) {
            CloudStore cloudStore = availableCloudStores.poll();
            if (cloudStore != null) {
                try {
                    logger.debug("Trying to upload slice '{}' to {}", sliceMetadata.getId(), cloudStore);

                    bytesUploaded = cloudStore.upload(sliceMetadata.getId(), new ByteBufferChannel(slice),
                        sliceMetadata.getSize());

                    uploaded = true;
                } catch (Exception e) {
                    logger.error("Failed to upload slice '" + sliceMetadata.getId() + "' to " + cloudStore, e);
                }
            } else {
                logger.error("No more available cloud stores to upload slice '{}'", sliceMetadata.getId());

                return null;
            }
        }

        logger.debug("Saving slice metadata {}", sliceMetadata);

        try {
            sliceMetadataRepository.save(sliceMetadata);
        } catch (Exception e) {
            logger.error("Failed to save slice metadata " + sliceMetadata, e);

            return null;
        }

        return bytesUploaded;
    }

    @Override
    public String toString() {
        return "SliceUploadTask{" +
            "sliceMetadata=" + sliceMetadata +
            ", sliceMetadataRepository=" + sliceMetadataRepository +
            ", availableCloudStores=" + availableCloudStores +
            '}';
    }

}
