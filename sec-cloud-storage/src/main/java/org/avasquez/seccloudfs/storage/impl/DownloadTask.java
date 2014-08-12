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
public class DownloadTask implements Callable<DownloadResult> {

    private static final Logger logger = LoggerFactory.getLogger(DownloadTask.class);

    private SliceMetadata sliceMetadata;
    private int sliceIndex;
    private boolean dataSlice;
    private int sliceSize;
    private CloudStore cloudStore;

    public DownloadTask(final SliceMetadata sliceMetadata, final int sliceIndex, final boolean dataSlice,
                        final int sliceSize, final CloudStore cloudStore) {
        this.sliceMetadata = sliceMetadata;
        this.sliceIndex = sliceIndex;
        this.dataSlice = dataSlice;
        this.sliceSize = sliceSize;
        this.cloudStore = cloudStore;
    }

    @Override
    public DownloadResult call() throws Exception {
        String sliceId = sliceMetadata.getId();
        String cloudStoreName = cloudStore.getName();

        logger.debug("Downloading slice '{}' from [{}]", sliceId, cloudStoreName);

        ByteBuffer slice = ByteBuffer.allocateDirect(sliceSize);

        try {
            cloudStore.download(sliceId, new ByteBufferChannel(slice));

            slice.clear();

            return new DownloadResult(slice, dataSlice, sliceIndex);
        } catch (Exception e) {
            logger.error("Failed to download slice '" + sliceId + "' from [" + cloudStoreName + "]", e);

            return null;
        }
    }

}
