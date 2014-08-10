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
    private CloudStore cloudStore;

    public SliceDownloadTask(final SliceMetadata sliceMetadata, final CloudStore cloudStore) {
        this.sliceMetadata = sliceMetadata;
        this.cloudStore = cloudStore;
    }

    @Override
    public ByteBuffer call() throws Exception {
        logger.debug("Downloading slice '{}' from {}", sliceMetadata.getId(), cloudStore);

        ByteBuffer slice = ByteBuffer.allocateDirect(sliceMetadata.getSize());

        try {
            cloudStore.download(sliceMetadata.getId(), new ByteBufferChannel(slice));

            return slice;
        } catch (Exception e) {
            logger.error("Failed to download slice '" + sliceMetadata.getId() + "' from " + cloudStore, e);

            return null;
        }
    }

    @Override
    public String toString() {
        return "SliceDownloadTask{" +
            "sliceMetadata=" + sliceMetadata +
            ", cloudStore=" + cloudStore +
            '}';
    }

}
