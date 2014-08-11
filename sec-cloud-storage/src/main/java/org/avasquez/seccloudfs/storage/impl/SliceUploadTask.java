package org.avasquez.seccloudfs.storage.impl;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.Callable;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.storage.db.model.SliceMetadata;
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
    private Queue<CloudStore> availableCloudStores;

    public SliceUploadTask(final ByteBuffer slice, final SliceMetadata sliceMetadata,
                           final Queue<CloudStore> availableCloudStores) {
        this.slice = slice;
        this.sliceMetadata = sliceMetadata;
        this.availableCloudStores = availableCloudStores;
    }

    @Override
    public Long call() throws Exception {
        String sliceId = sliceMetadata.getId();
        int sliceSize = slice.capacity();
        Long bytesUploaded = null;

        while (bytesUploaded != null) {
            CloudStore cloudStore = availableCloudStores.poll();
            if (cloudStore != null) {
                String cloudStoreName = cloudStore.getName();

                logger.debug("Trying to upload slice '{}' to [{}]", sliceId, cloudStoreName);

                try {
                    bytesUploaded = cloudStore.upload(sliceMetadata.getId(), new ByteBufferChannel(slice), sliceSize);

                    sliceMetadata.setCloudStoreName(cloudStore.getName());
                } catch (Exception e) {
                    logger.error("Failed to upload slice '" + sliceId + "' to [" + cloudStoreName + "]", e);
                }
            } else {
                logger.error("No more available cloud stores to upload slice '{}'", sliceId);

                break;
            }
        }

        return bytesUploaded;
    }

}
