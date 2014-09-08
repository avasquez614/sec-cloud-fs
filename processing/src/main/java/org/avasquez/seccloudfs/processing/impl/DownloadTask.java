package org.avasquez.seccloudfs.processing.impl;

import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.processing.db.model.SliceMetadata;
import org.avasquez.seccloudfs.utils.FileUtils;
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
    private CloudStore cloudStore;
    private Path sliceFile;

    public DownloadTask(SliceMetadata sliceMetadata, int sliceIndex, boolean dataSlice, CloudStore cloudStore,
                        Path sliceFile) {
        this.sliceMetadata = sliceMetadata;
        this.sliceIndex = sliceIndex;
        this.dataSlice = dataSlice;
        this.cloudStore = cloudStore;
        this.sliceFile = sliceFile;
    }

    @Override
    public DownloadResult call() throws Exception {
        String sliceId = sliceMetadata.getId();
        String cloudStoreName = cloudStore.getName();

        logger.debug("Downloading slice '{}' from [{}]", sliceId, cloudStoreName);

        try  {
            FileChannel channel = FileChannel.open(sliceFile, FileUtils.TMP_FILE_OPEN_OPTIONS);

            cloudStore.download(sliceId, channel);

            return new DownloadResult(channel, dataSlice, sliceIndex);
        } catch (Exception e) {
            logger.error("Failed to download slice '" + sliceId + "' from [" + cloudStoreName + "]", e);

            return null;
        }
    }

}
