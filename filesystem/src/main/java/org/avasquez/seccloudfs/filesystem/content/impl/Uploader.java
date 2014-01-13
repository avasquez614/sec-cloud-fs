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
    private ReadWriteLock readWriteLock;
    private CloudStore cloudStore;

    private boolean uploading;

    public Uploader(ContentMetadata metadata, ContentMetadataDao metadataDao, Path contentPath,
                    long timeoutForNextUpdate, Executor threadPool, ReadWriteLock readWriteLock,
                    CloudStore cloudStore) {
        this.metadata = metadata;
        this.metadataDao = metadataDao;
        this.contentPath = contentPath;
        this.timeoutForNextUpdate = timeoutForNextUpdate;
        this.threadPool = threadPool;
        this.readWriteLock = readWriteLock;
        this.cloudStore = cloudStore;
    }

    public synchronized void notifyUpdate() {
        if (!uploading) {
            runUploadInThread();
        }

        notify();
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
                while (!hasTimeoutOccurred()) {
                    wait(timeoutForNextUpdate);
                }
            } catch (InterruptedException e) {
                logger.error("Thread [" + Thread.currentThread().getName() + "] was interrupted while waiting " +
                        "for timeout for next update to occur", e);
            }
        }

        long snapshotTime;
        Path snapshotPath;

        readWriteLock.readLock().lock();
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
            readWriteLock.readLock().unlock();
        }

        try (FileChannel snapshotChannel = FileChannel.open(snapshotPath, StandardOpenOption.READ)) {
            cloudStore.upload(metadata.getId(), snapshotChannel, snapshotChannel.size());
        } catch (IOException e) {
            logger.error("Error while uploading file to cloud", e);

            return;
        }

        try {
            Files.delete(snapshotPath);
        } catch (IOException e) {
            logger.error("Unable to delete snapshot file '" + snapshotPath + "'", e);
        }

        metadata.setLastUpload(new Date());

        metadataDao.update(metadata);

        // Check if there where new updates while uploading
        if (getContentLastModifiedTime() > snapshotTime) {
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
