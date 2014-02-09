package org.avasquez.seccloudfs.filesystem.content.impl;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.filesystem.db.dao.ContentMetadataDao;
import org.avasquez.seccloudfs.filesystem.db.model.ContentMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.Lock;

/**
 * Created by alfonsovasquez on 11/01/14.
 */
public class Uploader {

    private static final Logger logger = LoggerFactory.getLogger(Uploader.class);

    private static final String SNAPSHOT_FILE_FORMAT = "%s.%d.snapshot";

    private ContentMetadata metadata;
    private ContentMetadataDao metadataDao;
    private Path downloadPath;
    private Lock accessLock;
    private Path snapshotDir;
    private long timeoutForNextUpdate;
    private ScheduledExecutorService executorService;
    private CloudStore cloudStore;

    private boolean uploading;

    public Uploader(ContentMetadata metadata, ContentMetadataDao metadataDao, Path downloadPath, Lock accessLock,
                    Path snapshotDir, long timeoutForNextUpdate, ScheduledExecutorService executorService,
                    CloudStore cloudStore) {
        this.metadata = metadata;
        this.metadataDao = metadataDao;
        this.downloadPath = downloadPath;
        this.snapshotDir = snapshotDir;
        this.timeoutForNextUpdate = timeoutForNextUpdate;
        this.executorService = executorService;
        this.accessLock = accessLock;
        this.cloudStore = cloudStore;
    }

    public synchronized void notifyUpdate() {
        if (!uploading) {
            runUploadInThread();
        } else {
            notify();
        }
    }

    private void runUploadInThread() {
        executorService.execute(new Runnable() {

            @Override
            public void run() {
                uploading = true;

                upload();

                uploading = false;
            }

        });
    }

    private void upload() {
        synchronized (this) {
            try {
                while (!metadata.isMarkedAsDeleted() && !hasTimeoutOccurred()) {
                    wait(timeoutForNextUpdate);
                }
            } catch (InterruptedException e) {
                logger.error("Thread [" + Thread.currentThread().getName() + "] was interrupted while waiting for " +
                        "timeout for next update to occur", e);
            }
        }

        if (!metadata.isMarkedAsDeleted() && metadata.getLastUploadTime() != null) {
            try {
                cloudStore.delete(metadata.getId());

                metadataDao.delete(metadata.getId());

                logger.info("Content '{}' deleted", metadata.getId());
            } catch (DbException e) {
                logger.error("Error while deleting content metadata '" + metadata.getId() + "' from DB", e);
            } catch (IOException e) {
                logger.error("Error while deleting content '" + metadata.getId() + "' from cloud", e);
            }

            return;
        }

        long snapshotTime;
        Path snapshotPath;

        accessLock.lock();
        try {
            snapshotTime = System.currentTimeMillis();
            snapshotPath = snapshotDir.resolve(String.format(SNAPSHOT_FILE_FORMAT, metadata.getId(), snapshotTime));

            try {
                Files.copy(downloadPath, snapshotPath);
            } catch (IOException e) {
                logger.error("Error while trying to create snapshot file " + snapshotPath, e);

                return;
            }
        } finally {
            accessLock.unlock();
        }

        logger.debug("Snapshot file {} created", snapshotPath);

        try (FileChannel snapshotChannel = FileChannel.open(snapshotPath, StandardOpenOption.READ)) {
            cloudStore.upload(metadata.getId(), snapshotChannel, snapshotChannel.size());
        } catch (IOException e) {
            logger.error("Error while uploading content '" + metadata.getId() + "' to cloud", e);

            return;
        }

        logger.info("Content '{}' uploaded", metadata.getId());

        try {
            metadata.setUploadedSize(Files.size(snapshotPath));
            metadata.setLastUploadTime(new Date());
        } catch (IOException e) {
            logger.error("Unable to retrieve file size for '" + snapshotPath + "'", e);

            return;
        }

        try {
            Files.delete(snapshotPath);
        } catch (IOException e) {
            logger.error("Unable to delete snapshot file '" + snapshotPath + "'", e);
        }

        logger.debug("Snapshot file {} deleted", snapshotPath);

        try {
            metadataDao.save(metadata);
        } catch (DbException e) {
            logger.error("Error while saving content metadata '" + metadata.getId() + "' to DB", e);

            return;
        }

        // Check if content was deleted or if there where new updates while uploading
        if (metadata.isMarkedAsDeleted() || getContentLastModifiedTime() > snapshotTime) {
            upload();
        }
    }

    private boolean hasTimeoutOccurred() {
        long now = System.currentTimeMillis();
        long lastModified = getContentLastModifiedTime();

        return (now - lastModified) >= timeoutForNextUpdate;
    }

    private long getContentLastModifiedTime() {
        try {
            return Files.getLastModifiedTime(downloadPath).toMillis();
        } catch (IOException e) {
            logger.error("Unable to retrieve last modified time for '" + downloadPath + "'", e);

            return 0;
        }
    }

}
