package org.avasquez.seccloudfs.processing.impl;

import java.nio.channels.ReadableByteChannel;
import java.util.Queue;
import java.util.concurrent.Callable;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.processing.db.model.SliceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Asynchronous task, implemented as a {@link java.util.concurrent.Callable}, to upload a slice to a
 * {@link org.avasquez.seccloudfs.cloud.CloudStore}.
 *
 * @author avasquez
 */
public class UploadTask implements Callable<Boolean> {

    private static final Logger logger = LoggerFactory.getLogger(UploadTask.class);

    private ReadableByteChannel slice;
    private int sliceSize;
    private SliceMetadata sliceMetadata;
    private Queue<CloudStore> availableCloudStores;

    public UploadTask(ReadableByteChannel slice, int sliceSize, SliceMetadata sliceMetadata,
                      Queue<CloudStore> availableCloudStores) {
        this.slice = slice;
        this.sliceSize = sliceSize;
        this.sliceMetadata = sliceMetadata;
        this.availableCloudStores = availableCloudStores;
    }

    @Override
    public Boolean call() throws Exception {
        String sliceId = sliceMetadata.getId();
        boolean uploaded = false;

        while (!uploaded) {
            CloudStore cloudStore = availableCloudStores.poll();
            if (cloudStore != null) {
                String cloudStoreName = cloudStore.getName();

                logger.debug("Trying to upload slice '{}' to [{}]", sliceId, cloudStoreName);

                try {
                    cloudStore.upload(sliceMetadata.getId(), slice, sliceSize);

                    sliceMetadata.setCloudStoreName(cloudStore.getName());

                    uploaded = true;
                } catch (Exception e) {
                    logger.error("Failed to upload slice '" + sliceId + "' to [" + cloudStoreName + "]", e);
                }
            } else {
                logger.error("No more available cloud stores to upload slice '{}'", sliceId);

                break;
            }
        }

        return uploaded;
    }

}
