package org.avasquez.seccloudfs.filesystem.content.impl;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.filesystem.db.model.ContentMetadata;
import org.avasquez.seccloudfs.filesystem.db.repos.ContentMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * Created by alfonsovasquez on 11/01/14.
 */
public class Uploader {

    private static final Logger logger = LoggerFactory.getLogger(Uploader.class);

    private static final String SNAPSHOT_FILE_FORMAT = "%s.%d.snapshot";

    private ContentMetadata metadata;
    private ContentMetadataRepository metadataRepo;
    private CloudStore cloudStore;
    private Path downloadPath;
    private Lock accessLock;
    private Path snapshotDir;
    private ScheduledExecutorService executorService;
    private long timeoutForNextUpdateSecs;
    private long retryDelaySecs;

    private volatile boolean uploading;

    public Uploader(ContentMetadata metadata, ContentMetadataRepository metadataRepo, CloudStore cloudStore,
                    Path downloadPath, Lock accessLock, Path snapshotDir, ScheduledExecutorService executorService,
                    long timeoutForNextUpdateSecs, long retryDelaySecs) {
        this.metadata = metadata;
        this.metadataRepo = metadataRepo;
        this.cloudStore = cloudStore;
        this.downloadPath = downloadPath;
        this.snapshotDir = snapshotDir;
        this.timeoutForNextUpdateSecs = timeoutForNextUpdateSecs;
        this.executorService = executorService;
        this.accessLock = accessLock;
        this.retryDelaySecs = retryDelaySecs;
    }

    public synchronized void notifyUpdate() {
        if (!uploading) {
            uploading = true;

            runUploadInThread();
        } else {
            notify();
        }
    }

    private void runUploadInThread() {
        executorService.execute(new Runnable() {

            @Override
            public void run() {
                logger.debug("Uploading thread for content '{}' started", metadata.getId());

                uploadUntilNoUpdatesReceived();

                uploading = false;
            }

        });
    }

    private void uploadUntilNoUpdatesReceived() {
        synchronized (this) {
            try {
                while (!metadata.isMarkedAsDeleted() && !hasTimeoutOccurred()) {
                    logger.trace("Update received for content '{}'. Waiting {} secs for next update",
                            metadata.getId(), timeoutForNextUpdateSecs);

                    wait(TimeUnit.SECONDS.toMillis(timeoutForNextUpdateSecs));
                }
            } catch (InterruptedException | UploadFailedException e) {
                logger.error("Error while waiting for content " + metadata + " updates before upload", e);
            }
        }

        try {
            if (metadata.isMarkedAsDeleted()) {
                delete();
            } else {
                upload();
            }
        } catch (UploadFailedException e) {
            logger.error("Upload for content " + metadata + " failed. Retrying in " + retryDelaySecs + " seconds", e);

            retryUpload();
        }
    }

    private void delete() throws UploadFailedException {
        logger.info("Delete for content '{}' started", metadata.getId());

        try {
            if (metadata.getLastUploadTime() != null) {
                cloudStore.delete(metadata.getId());
            }

            metadataRepo.delete(metadata.getId());

            logger.info("Delete for content '{}' finished", metadata.getId());

            return;
        } catch (DbException e) {
            throw new UploadFailedException("Error while deleting content from DB", e);
        } catch (IOException e) {
            throw new UploadFailedException("Error while deleting content from cloud", e);
        }
    }

    private void upload() throws UploadFailedException {
        logger.info("Upload for content '{}' started", metadata.getId());

        long snapshotTime;
        Path snapshotPath;

        accessLock.lock();
        try {
            snapshotTime = System.currentTimeMillis();
            snapshotPath = snapshotDir.resolve(String.format(SNAPSHOT_FILE_FORMAT, metadata.getId(), snapshotTime));

            try {
                Files.copy(downloadPath, snapshotPath);
            } catch (IOException e) {
                throw new UploadFailedException("Error while trying to create snapshot file " + snapshotPath, e);
            }
        } finally {
            accessLock.unlock();
        }

        logger.debug("Snapshot file {} created", snapshotPath);

        try (FileChannel snapshotChannel = FileChannel.open(snapshotPath, StandardOpenOption.READ)) {
            cloudStore.upload(metadata.getId(), snapshotChannel, snapshotChannel.size());
        } catch (IOException e) {
            throw new UploadFailedException("Error while uploading content to cloud", e);
        }

        logger.info("Content '{}' uploaded to cloud", metadata.getId());

        try {
            metadata.setUploadedSize(Files.size(snapshotPath));
            metadata.setLastUploadTime(new Date());
        } catch (IOException e) {
            throw new UploadFailedException("Unable to retrieve file size for " + snapshotPath, e);
        }

        try {
            Files.delete(snapshotPath);
        } catch (IOException e) {
            logger.warn("Unable to delete snapshot file " + snapshotPath, e);
        }

        logger.debug("Snapshot file {} deleted", snapshotPath);

        try {
            metadataRepo.save(metadata);
        } catch (DbException e) {
            throw new UploadFailedException("Error while saving content metadata to DB", e);
        }

        logger.info("Upload for content '{}' finished", metadata.getId());

        // Check if content was deleted or if there where new updates while uploading
        if (metadata.isMarkedAsDeleted() || getContentLastModifiedTime() > snapshotTime) {
            uploadUntilNoUpdatesReceived();
        }
    }

    private boolean hasTimeoutOccurred() throws UploadFailedException {
        long now = System.currentTimeMillis();
        long lastModified = getContentLastModifiedTime();

        return TimeUnit.MILLISECONDS.toSeconds(now - lastModified) >= timeoutForNextUpdateSecs;
    }

    private long getContentLastModifiedTime() throws UploadFailedException {
        try {
            return Files.getLastModifiedTime(downloadPath).toMillis();
        } catch (IOException e) {
            throw new UploadFailedException("Unable to retrieve last modified time for '" + downloadPath + "'", e);
        }
    }

    private void retryUpload() {
        executorService.schedule(new Runnable() {

            @Override
            public void run() {
                logger.info("Retrying upload for content '{}'", metadata.getId());

                notifyUpdate();
            }

        }, retryDelaySecs, TimeUnit.SECONDS);
    }

    private static class UploadFailedException extends IOException {

        private UploadFailedException(String message, Throwable cause) {
            super(message, cause);
        }

    }

}
