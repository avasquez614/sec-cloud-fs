package org.avasquez.seccloudfs.filesystem.content.impl;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.filesystem.content.CloudContent;
import org.avasquez.seccloudfs.filesystem.db.model.ContentMetadata;
import org.avasquez.seccloudfs.filesystem.db.repos.ContentMetadataRepository;
import org.avasquez.seccloudfs.filesystem.util.FlushableByteChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.locks.Lock;

/**
 * Created by alfonsovasquez on 11/01/14.
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

    private volatile int openChannels;

    public CloudContentImpl(ContentMetadata metadata, ContentMetadataRepository metadataRepo, Path downloadPath,
                            Lock accessLock, CloudStore cloudStore, Uploader uploader) throws IOException {
        this.metadata = metadata;
        this.metadataRepo = metadataRepo;
        this.downloadPath = downloadPath;
        this.accessLock = accessLock;
        this.cloudStore = cloudStore;
        this.uploader = uploader;
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
            FileTime lastUploadTime = FileTime.fromMillis(metadata.getLastUploadTime().getTime());
            FileTime lastModifiedTime = Files.getLastModifiedTime(downloadPath);

            accessLock.lock();
            try {
                // If no updates are pending and there are no open channels, delete.
                if (lastModifiedTime.compareTo(lastUploadTime) < 0 && openChannels == 0) {
                    Files.deleteIfExists(downloadPath);

                    logger.info("Download for {} deleted", this);

                    return true;
                }
            } finally {
                accessLock.unlock();
            }
        }

        return false;
    }

    public void delete() throws IOException {
        metadata.setMarkedAsDeleted(true);
        metadataRepo.save(metadata);

        logger.info("{} marked as deleted", this);

        accessLock.lock();
        try {
            if (openChannels == 0) {
                Files.deleteIfExists(downloadPath);

                uploader.notifyUpdate();

                logger.info("Download for {} deleted", this);
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
                        download();
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

    private void download() throws IOException {
        Path tmpPath = Files.createTempFile(TMP_FILE_PREFIX, TMP_FILE_SUFFIX);

        try {
            try (FileChannel tmpFile = FileChannel.open(tmpPath, StandardOpenOption.WRITE)) {
                cloudStore.download(metadata.getId(), tmpFile);
            }

            Files.move(tmpPath, downloadPath, StandardCopyOption.ATOMIC_MOVE);

            logger.info("{} downloaded", this);
        } catch (IOException e) {
            throw new IOException("Error while trying to download " + this + " from cloud", e);
        }
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

                            logger.info("Download for {} deleted", this);
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
