package org.avasquez.seccloudfs.storage.impl;

import java.nio.ByteBuffer;
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
public class SliceUploadTask implements Callable<Boolean> {

    private static final Logger logger = LoggerFactory.getLogger(SliceUploadTask.class);

    private ByteBuffer slice;
    private SliceMetadata sliceMetadata;
    private SliceMetadataRepository sliceMetadataRepository;
    private CloudStore cloudStore;

    public SliceUploadTask(final ByteBuffer slice, final SliceMetadata sliceMetadata,
                           final SliceMetadataRepository sliceMetadataRepository, final CloudStore cloudStore) {
        this.slice = slice;
        this.sliceMetadata = sliceMetadata;
        this.sliceMetadataRepository = sliceMetadataRepository;
        this.cloudStore = cloudStore;
    }

    @Override
    public Boolean call() throws Exception {
        logger.debug("Uploading slice '{}' to {}", sliceMetadata.getId(), cloudStore);

        try {
            cloudStore.upload(sliceMetadata.getId(), new ByteBufferChannel(slice), sliceMetadata.getSize());
        } catch (Exception e) {
            logger.error("Failed to upload slice '" + sliceMetadata.getId() + "' to " + cloudStore, e);

            return false;
        }

        logger.debug("Saving slice metadata {}", sliceMetadata);

        try {
            sliceMetadataRepository.save(sliceMetadata);
        } catch (Exception e) {
            logger.error("Failed to save slice metadata " + sliceMetadata, e);

            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "SliceUploadTask{" +
            "sliceMetadata=" + sliceMetadata +
            ", sliceMetadataRepository=" + sliceMetadataRepository +
            ", cloudStore=" + cloudStore +
            '}';
    }

}
