package org.avasquez.seccloudfs.filesystem.content.impl;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.filesystem.db.dao.ContentMetadataDao;
import org.avasquez.seccloudfs.filesystem.db.model.ContentMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Created by alfonsovasquez on 11/01/14.
 */
public class Uploader {

    private static final Logger logger = LoggerFactory.getLogger(Uploader.class);

    private static final String SNAPSHOT_PREFIX_FORMAT = ".%s.snapshot";

    private ContentMetadata metadata;
    private ContentMetadataDao metadataDao;
    private Path contentPath;
    private long timeoutForNextUpdate;
    private Executor threadPool;
    private ReadWriteLock rwLock;
    private CloudStore cloudStore;

    private boolean uploading;

    public Uploader(ContentMetadata metadata, ContentMetadataDao metadataDao, Path contentPath,
                    long timeoutForNextUpdate, Executor threadPool, ReadWriteLock rwLock, CloudStore cloudStore) {
        this.metadata = metadata;
        this.metadataDao = metadataDao;
        this.contentPath = contentPath;
        this.timeoutForNextUpdate = timeoutForNextUpdate;
        this.threadPool = threadPool;
        this.rwLock = rwLock;
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
        threadPool.execute(new Runnable() {

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
            } catch (IOException e) {
                logger.error("Error while deleting content '" + metadata.getId() + "' from cloud", e);
            }

            metadataDao.delete(metadata.getId());

            return;
        }

        long snapshotTime;
        Path snapshotPath;

        rwLock.readLock().lock();
        try {
            snapshotTime = System.currentTimeMillis();
            snapshotPath = Paths.get(contentPath + String.format(SNAPSHOT_PREFIX_FORMAT, snapshotTime));

            try {
                Files.copy(contentPath, snapshotPath);
            } catch (IOException e) {
                logger.error("Error while trying to create snapshot file '" + snapshotPath + "'", e);

                return;
            }
        } finally {
            rwLock.readLock().unlock();
        }

        try (FileChannel snapshotChannel = FileChannel.open(snapshotPath, StandardOpenOption.READ)) {
            cloudStore.upload(metadata.getId(), snapshotChannel, snapshotChannel.size());
        } catch (IOException e) {
            logger.error("Error while uploading content '" + metadata.getId() + "' to cloud", e);

            return;
        }

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

        metadataDao.save(metadata);

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
            return Files.getLastModifiedTime(contentPath).toMillis();
        } catch (IOException e) {
            logger.error("Unable to retrieve last modified time for '" + contentPath + "'", e);

            return 0;
        }
    }

}
