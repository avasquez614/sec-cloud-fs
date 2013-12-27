package org.avasquez.seccloudfs.filesystem.impl;

import org.avasquez.seccloudfs.filesystem.FileContent;
import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;
import org.avasquez.seccloudfs.secure.storage.SecureCloudStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.BitSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Receives updates of a file, resolves which chunks of the file where updated, and waits for the next update for a
 * certain period of time. If no new update is received during that period, it proceeds to save the updated chunks
 * (or delete if the update is a delete) to the cloud through the
 * {@link org.avasquez.seccloudfs.secure.storage.SecureCloudStorage}.
 *
 * @author avasquez
 */
public class CloudStorageUpdater implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(CloudStorageUpdater.class);

    private MetadataAwareFile file;
    private long nextUpdateTimeout;
    private SecureCloudStorage cloudStorage;
    private Executor executor;

    private BlockingQueue<FileUpdate> fileUpdates;
    private BitSet updatedChunks;
    private BitSet deletedChunks;
    private volatile boolean running;

    public CloudStorageUpdater(MetadataAwareFile file, long nextUpdateTimeout, SecureCloudStorage cloudStorage,
                               Executor executor) {
        this.file = file;
        this.nextUpdateTimeout = nextUpdateTimeout;
        this.cloudStorage = cloudStorage;
        this.executor = executor;
        this.fileUpdates = new LinkedBlockingQueue<FileUpdate>();
        this.running = false;
    }

    public void addUpdate(FileUpdate fileUpdate) {
        fileUpdates.add(fileUpdate);

        if (!running) {
            synchronized (this) {
                if (!running) {
                    running = true;

                    executor.execute(this);
                }
            }
        }
    }

    @Override
    public void run() {
        FileUpdate fileUpdate;
        FileMetadata metadata = file.getMetadata();
        long chunkSize = metadata.getChunkSize();

        try {
            while ((fileUpdate = fileUpdates.poll(nextUpdateTimeout, TimeUnit.SECONDS)) != null) {
                long endPosition = fileUpdate.getPosition() + fileUpdate.getLength() - 1;
                int startChunk = metadata.getChunkForPosition(fileUpdate.getPosition());
                int endChunk = metadata.getChunkForPosition(endPosition);

                if (fileUpdate.isDelete()) {
                    if (deletedChunks == null) {
                        deletedChunks = new BitSet();
                    }

                    deletedChunks.set(startChunk, endChunk + 1);
                } else {
                    if (updatedChunks == null) {
                        updatedChunks = new BitSet();
                    }

                    updatedChunks.set(startChunk, endChunk + 1);
                }
            }

            if (updatedChunks != null && updatedChunks.cardinality() > 0) {
                try {
                    FileContent content = file.getContent();

                    for (int i = updatedChunks.nextSetBit(0); i >= 0; i = updatedChunks.nextSetBit(i + 1)) {
                        content.setPosition(i * chunkSize);

                        cloudStorage.storeData(metadata.getChunkName(i), content, chunkSize);
                    }
                } catch (Exception e) {
                    logger.error("Error while trying to store data in cloud", e);
                }
            }
            if (deletedChunks != null && deletedChunks.cardinality() > 0) {
                try {
                    for (int i = deletedChunks.nextSetBit(0); i >= 0; i = deletedChunks.nextSetBit(i + 1)) {
                        cloudStorage.deleteData(metadata.getChunkName(i));
                    }
                } catch (Exception e) {
                    logger.error("Error while trying to delete data in cloud", e);
                }
            }
        } catch (InterruptedException e) {
            logger.error(String.format("The thread was interrupted while waiting for new update of file %s", file), e);
        }

        running = false;
        updatedChunks = null;
        deletedChunks = null;
    }

}
