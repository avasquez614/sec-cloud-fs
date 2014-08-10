package org.avasquez.seccloudfs.storage.impl;

import java.util.concurrent.Callable;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.storage.db.model.SliceMetadata;
import org.avasquez.seccloudfs.storage.db.repos.SliceMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Asynchronous task, implemented as a {@link java.util.concurrent.Callable}, to delete a slice from a
 * {@link org.avasquez.seccloudfs.cloud.CloudStore}.
 *
 * @author avasquez
 */
public class SliceDeleteTask implements Callable<Boolean> {

    private static final Logger logger = LoggerFactory.getLogger(SliceDeleteTask.class);

    private SliceMetadata sliceMetadata;
    private SliceMetadataRepository sliceMetadataRepository;
    private CloudStore cloudStore;

    public SliceDeleteTask(final SliceMetadata sliceMetadata, final SliceMetadataRepository sliceMetadataRepository,
                           final CloudStore cloudStore) {
        this.sliceMetadata = sliceMetadata;
        this.sliceMetadataRepository = sliceMetadataRepository;
        this.cloudStore = cloudStore;
    }

    @Override
    public Boolean call() throws Exception {
        logger.debug("Deleting slice metadata {}", sliceMetadata);

        try {
            sliceMetadataRepository.delete(sliceMetadata.getId());
        } catch (Exception e) {
            logger.error("Failed to delete slice metadata " + sliceMetadata, e);

            return false;
        }

        logger.debug("Deleting slice '{}' from {}", sliceMetadata.getId(), cloudStore);

        try {
            cloudStore.delete(sliceMetadata.getId());

            return true;
        } catch (Exception e) {
            logger.error("Failed to delete slice '" + sliceMetadata.getId() + "' from " + cloudStore, e);

            return false;
        }
    }

    @Override
    public String toString() {
        return "SliceDeleteTask{" +
            "sliceMetadata=" + sliceMetadata +
            ", sliceMetadataRepository=" + sliceMetadataRepository +
            ", cloudStore=" + cloudStore +
            '}';
    }

}
