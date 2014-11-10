package org.avasquez.seccloudfs.filesystem.content.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.filesystem.content.CloudContent;
import org.avasquez.seccloudfs.filesystem.db.model.ContentMetadata;
import org.avasquez.seccloudfs.filesystem.db.repos.ContentMetadataRepository;
import org.avasquez.seccloudfs.filesystem.util.FlushableByteChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link org.avasquez.seccloudfs.filesystem.content.CloudContent}, which downloads
 * directly the content when not cached, and starts a new {@link org.avasquez.seccloudfs.filesystem.content.impl
 * .Uploader} when the content has been modified.
 *
 * @author avasquez
 */
public class CloudContentImpl implements CloudContent {

    private static final Logger logger = LoggerFactory.getLogger(CloudContentImpl.class);

    private static final String TMP_FILE_PREFIX =   "seccloudfs-";
    private static final String TMP_FILE_SUFFIX =   ".download";

    private ContentMetadata metadata;
    private ContentMetadataRepository metadataRepo;
    private Path downloadPath;
    private CloudStore cloudStore;
    private Uploader uploader;
    private Lock accessLock;
    private long retryDownloadDelaySecs;
    private int maxDownloadRetries;

    private volatile int openChannels;
    private int currentDownloadRetries;

    public CloudContentImpl(ContentMetadata metadata, ContentMetadataRepository metadataRepo, Path downloadPath,
                            Lock accessLock, CloudStore cloudStore, Uploader uploader, long retryDownloadDelaySecs,
                            int maxDownloadRetries)
        throws IOException {
        this.metadata = metadata;
        this.metadataRepo = metadataRepo;
        this.downloadPath = downloadPath;
        this.accessLock = accessLock;
        this.cloudStore = cloudStore;
        this.uploader = uploader;
        this.retryDownloadDelaySecs = retryDownloadDelaySecs;
        this.maxDownloadRetries = maxDownloadRetries;
    }

    @Override
    public String getId() {
        return metadata.getId();
    }

    @Override
    public long getSize() throws IOException {
        if (Files.exists(downloadPath)) {
            return Files.size(downloadPath);
        } else {
            return metadata.getUploadedSize();
        }
    }

    @Override
    public FlushableByteChannel getByteChannel() throws IOException {
        if (metadata.isMarkedAsDeleted()) {
            throw new IOException("Content " + this + " deleted");
        }

        return new ContentByteChannel();
    }

    @Override
    public boolean isDownloaded() {
        return Files.exists(downloadPath);
    }

    @Override
    public boolean deleteDownload() throws IOException {
        if (isDownloaded()) {
            FileTime lastUploadTime = metadata.getLastUploadTime() != null ? FileTime.fromMillis(metadata
                    .getLastUploadTime().getTime()) : null;
            FileTime lastModifiedTime = Files.getLastModifiedTime(downloadPath);

            accessLock.lock();
            try {
                // If no updates are pending and there are no open channels, delete.
                if (lastUploadTime != null && lastModifiedTime.compareTo(lastUploadTime) < 0 && openChannels == 0) {
                    Files.deleteIfExists(downloadPath);

                    logger.info("Download for content '{}' deleted", getId());

                    return true;
                }
            } finally {
                accessLock.unlock();
            }
        }

        return false;
    }

    @Override
    public void forceUpload() throws IOException {
        if (isDownloaded()) {
            accessLock.lock();
            try {
                uploader.notifyUpdate();

                logger.info("Forced upload of content '{}'", getId());
            } finally {
                accessLock.unlock();
            }
        }
    }

    public void delete() throws IOException {
        metadata.setMarkedAsDeleted(true);
        try {
            metadataRepo.save(metadata);
        } catch (DbException e) {
            throw new IOException("Unable to save " + metadata + " in DB", e);
        }

        logger.info("Content '{}' marked as deleted", getId());

        accessLock.lock();
        try {
            if (openChannels == 0) {
                Files.deleteIfExists(downloadPath);

                uploader.notifyUpdate();

                logger.info("Download for content '{}' deleted", getId());
            }
        } finally {
            accessLock.unlock();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CloudContentImpl content = (CloudContentImpl) o;

        if (!metadata.equals(content.metadata)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return metadata.hashCode();
    }

    @Override
    public String toString() {
        return "CloudContentImpl{" +
                "metadata=" + metadata +
                ", downloadPath=" + downloadPath +
                '}';
    }

    private void checkDownloaded() throws IOException {
        if (!metadata.isMarkedAsDeleted() && !Files.exists(downloadPath)) {
            if (metadata.getLastUploadTime() != null) {
                // File has not been downloaded, so download it
                accessLock.lock();
                try {
                    if (!metadata.isMarkedAsDeleted() && !Files.exists(downloadPath)) {
                        downloadAndRetry();
                    }
                } finally {
                    accessLock.unlock();
                }
            } else {
                // It's new content, so create the file
                Files.createFile(downloadPath);
            }
        }
    }

    private void downloadAndRetry() throws IOException {
        currentDownloadRetries = 0;

        while (currentDownloadRetries <= maxDownloadRetries) {
            try {
                download();
            } catch (IOException e) {
                logger.error("Error while trying to download content '" + getId() + "' from cloud", e);

                currentDownloadRetries++;

                if (currentDownloadRetries <= maxDownloadRetries) {
                    try {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(retryDownloadDelaySecs));
                    } catch (InterruptedException ie) {
                        logger.debug("Thread interrupted while waiting for download delay", ie);
                    }

                    logger.info("Retry #{} of content '{}' download", currentDownloadRetries, getId());
                }
            }
        }

        if (currentDownloadRetries > maxDownloadRetries) {
            throw new IOException("Max retries for download of content '" + getId() + "' reached");
        }
    }

    private void download() throws IOException {
        Path tmpPath = Files.createTempFile(TMP_FILE_PREFIX, TMP_FILE_SUFFIX);

        try (FileChannel tmpFile = FileChannel.open(tmpPath, StandardOpenOption.WRITE)) {
            cloudStore.download(metadata.getId(), tmpFile);
        }

        Files.move(tmpPath, downloadPath, StandardCopyOption.ATOMIC_MOVE);

        logger.info("Content '{}' downloaded", getId());
    }

    private class ContentByteChannel implements FlushableByteChannel {

        private FileChannel fileChannel;
        private boolean open;

        private ContentByteChannel() throws IOException {
            accessLock.lock();
            try {
                open = true;
                openChannels++;
            } finally {
                accessLock.unlock();
            }
        }

        @Override
        public long position() throws IOException {
            initFileChannel();

            return fileChannel.position();
        }

        @Override
        public SeekableByteChannel position(long newPosition) throws IOException {
            initFileChannel();

            return fileChannel.position(newPosition);
        }

        @Override
        public long size() throws IOException {
            initFileChannel();

            return fileChannel.size();
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void close() throws IOException {
            if (open) {
                if (fileChannel != null) {
                    fileChannel.close();
                }

                accessLock.lock();
                try {
                    try {
                        if (metadata.isMarkedAsDeleted() && openChannels == 1) {
                            Files.deleteIfExists(downloadPath);

                            uploader.notifyUpdate();

                            logger.info("Download for content '{}' deleted", getId());
                        }
                    } finally {
                        openChannels--;
                        open = false;
                    }
                } finally {
                    accessLock.unlock();
                }
            }
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            initFileChannel();

            accessLock.lock();
            try {
                return fileChannel.read(dst);
            } finally {
                accessLock.unlock();
            }
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            int bytesWritten;

            initFileChannel();

            accessLock.lock();
            try {
                bytesWritten = fileChannel.write(src);
            } finally {
                accessLock.unlock();
            }

            uploader.notifyUpdate();

            return bytesWritten;
        }

        @Override
        public SeekableByteChannel truncate(long size) throws IOException {
            initFileChannel();

            accessLock.lock();
            try {
                fileChannel.truncate(size);
            } finally {
                accessLock.unlock();
            }

            uploader.notifyUpdate();

            return this;
        }

        @Override
        public void flush() throws IOException {
            if (fileChannel != null) {
                fileChannel.force(true);
            }
        }

        private void initFileChannel() throws IOException {
            if (fileChannel == null) {
                checkDownloaded();

                fileChannel = FileChannel.open(downloadPath, StandardOpenOption.READ, StandardOpenOption.WRITE);
            }
        }
    }

}
