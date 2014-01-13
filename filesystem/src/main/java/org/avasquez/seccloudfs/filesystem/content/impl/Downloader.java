package org.avasquez.seccloudfs.filesystem.content.impl;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;

/**
 * Created by alfonsovasquez on 09/01/14.
 */
public class Downloader {

    private static final Logger logger = LoggerFactory.getLogger(Downloader.class);

    private static final String TMP_PATH_SUFFIX = ".download";

    private String contentId;
    private Path downloadPath;
    private CloudStore cloudStore;

    private volatile boolean downloading;

    public Downloader(String contentId, Path downloadPath, CloudStore cloudStore) {
        this.contentId = contentId;
        this.downloadPath = downloadPath;
        this.cloudStore = cloudStore;
    }

    public boolean isContentDownloading() {
        return downloading;
    }

    public synchronized void awaitTillDownloadFinished() {
        try {
            wait();
        } catch (InterruptedException e) {
            logger.error("Thread [" + Thread.currentThread().getName() + "] was interrupted while waiting " +
                    "for download to finish", e);
        }
    }

    public void download() throws IOException {
        downloading = true;

        Path tmpPath = Paths.get(downloadPath + TMP_PATH_SUFFIX);

        try (FileChannel tmpFile = FileChannel.open(tmpPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            cloudStore.download(contentId, tmpFile);
        }

        Files.move(tmpPath, downloadPath, StandardCopyOption.ATOMIC_MOVE);

        synchronized (this) {
            notifyAll();
        }

        downloading = false;
    }

}
