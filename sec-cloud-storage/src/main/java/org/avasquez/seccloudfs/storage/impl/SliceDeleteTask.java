package org.avasquez.seccloudfs.storage.impl;

import java.util.concurrent.Callable;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.storage.db.model.SliceMetadata;
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
    private CloudStore cloudStore;

    public SliceDeleteTask(final SliceMetadata sliceMetadata, final CloudStore cloudStore) {
        this.sliceMetadata = sliceMetadata;
        this.cloudStore = cloudStore;
    }

    @Override
    public Boolean call() throws Exception {
        String sliceId = sliceMetadata.getId();
        String cloudStoreName = cloudStore.getName();

        logger.debug("Deleting slice '{}' from [{}]", sliceId, cloudStoreName);

        try {
            cloudStore.delete(sliceId);

            return true;
        } catch (Exception e) {
            logger.error("Failed to delete slice '" + sliceId + "' from [" + cloudStoreName + "]", e);

            return false;
        }
    }

}
