package org.avasquez.seccloudfs.storage.impl;

import java.nio.ByteBuffer;
import java.util.concurrent.Callable;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.storage.db.model.SliceMetadata;
import org.avasquez.seccloudfs.utils.nio.ByteBufferChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Asynchronous task, implemented as a {@link java.util.concurrent.Callable}, to download a slice from a
 * {@link org.avasquez.seccloudfs.cloud.CloudStore}.
 *
 * @author avasquez
 */
public class SliceDownloadTask implements Callable<ByteBuffer> {

    private static final Logger logger = LoggerFactory.getLogger(SliceDownloadTask.class);

    private SliceMetadata sliceMetadata;
    private int sliceSize;
    private CloudStore cloudStore;

    public SliceDownloadTask(final SliceMetadata sliceMetadata, final int sliceSize, final CloudStore cloudStore) {
        this.sliceMetadata = sliceMetadata;
        this.sliceSize = sliceSize;
        this.cloudStore = cloudStore;
    }

    @Override
    public ByteBuffer call() throws Exception {
        String sliceId = sliceMetadata.getId();
        String cloudStoreName = cloudStore.getName();

        logger.debug("Downloading slice '{}' from [{}]", sliceId, cloudStoreName);

        ByteBuffer slice = ByteBuffer.allocateDirect(sliceSize);

        try {
            cloudStore.download(sliceId, new ByteBufferChannel(slice));

            return slice;
        } catch (Exception e) {
            logger.error("Failed to download slice '" + sliceId + "' from [" + cloudStoreName + "]", e);

            return null;
        }
    }

}
