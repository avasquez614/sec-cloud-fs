package org.avasquez.seccloudfs.filesystem.content.impl;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.filesystem.content.Content;
import org.avasquez.seccloudfs.filesystem.db.dao.ContentMetadataDao;
import org.avasquez.seccloudfs.filesystem.db.model.ContentMetadata;
import org.avasquez.seccloudfs.filesystem.util.SyncAwareByteChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Created by alfonsovasquez on 11/01/14.
 */
public class CloudContent implements Content {

    private static final String TMP_PATH_SUFFIX = ".download";

    private ContentMetadata metadata;
    private ContentMetadataDao metadataDao;
    private Path path;
    private CloudStore cloudStore;
    private Uploader uploader;
    private ReadWriteLock rwLock;

    private AtomicInteger openChannels;

    public CloudContent(ContentMetadata metadata, ContentMetadataDao metadataDao, Path path, CloudStore cloudStore,
                        Uploader uploader, ReadWriteLock rwLock) throws IOException {
        this.metadata = metadata;
        this.metadataDao = metadataDao;
        this.path = path;
        this.cloudStore = cloudStore;
        this.uploader = uploader;
        this.rwLock = rwLock;
        this.openChannels = new AtomicInteger();
    }

    @Override
    public String getId() {
        return metadata.getId();
    }

    @Override
    public long getSize() throws IOException {
        if (Files.exists(path)) {
            return Files.size(path);
        } else {
            return metadata.getUploadedSize();
        }
    }

    @Override
    public SyncAwareByteChannel getByteChannel() throws IOException {
        checkDownloaded();

        if (metadata.isMarkedAsDeleted()) {
            throw new IOException("Content '" + metadata.getId() + "' deleted");
        }

        return new ContentByteChannel();
    }

    public void delete() throws IOException {
        metadata.setMarkedAsDeleted(true);
        metadataDao.save(metadata);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CloudContent content = (CloudContent) o;

        if (!metadata.equals(content.metadata)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return metadata.hashCode();
    }

    private void checkDownloaded() throws IOException {
        if (!metadata.isMarkedAsDeleted() && !Files.exists(path)) {
            if (metadata.getLastUploadTime() != null) {
                // File has not been downloaded, so download it
                rwLock.writeLock().lock();
                try {
                    if (!metadata.isMarkedAsDeleted() && !Files.exists(path)) {
                        download();
                    }
                } finally {
                    rwLock.writeLock().unlock();
                }
            } else {
                // It's new content, so create the file
                Files.createFile(path);
            }
        }
    }

    private void download() throws IOException {
        Path tmpPath = Paths.get(path + TMP_PATH_SUFFIX);

        try {
            try (FileChannel tmpFile = FileChannel.open(tmpPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                cloudStore.download(metadata.getId(), tmpFile);
            }

            Files.move(tmpPath, path, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new IOException("Error while trying to download content '" + metadata.getId() + "' from cloud", e);
        }
    }

    private class ContentByteChannel implements SyncAwareByteChannel {

        private FileChannel fileChannel;

        private ContentByteChannel() throws IOException {
            fileChannel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE);

            openChannels.incrementAndGet();
        }

        @Override
        public long position() throws IOException {
            return fileChannel.position();
        }

        @Override
        public SeekableByteChannel position(long newPosition) throws IOException {
            return fileChannel.position(newPosition);
        }

        @Override
        public long size() throws IOException {
            return fileChannel.size();
        }

        @Override
        public boolean isOpen() {
            return fileChannel.isOpen();
        }

        @Override
        public void close() throws IOException {
            openChannels.decrementAndGet();

            rwLock.writeLock().lock();
            try {
                fileChannel.close();

                if (metadata.isMarkedAsDeleted() && openChannels.get() <= 0) {
                    Files.deleteIfExists(path);

                    uploader.notifyUpdate();
                }
            } finally {
                rwLock.writeLock().unlock();
            }
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            rwLock.readLock().lock();
            try {
                return fileChannel.read(dst);
            } finally {
                rwLock.readLock().unlock();
            }
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            int bytesWritten;

            rwLock.writeLock().lock();
            try {
                bytesWritten = fileChannel.write(src);
            } finally {
                rwLock.writeLock().unlock();
            }

            uploader.notifyUpdate();

            return bytesWritten;
        }

        @Override
        public SeekableByteChannel truncate(long size) throws IOException {
            rwLock.writeLock().lock();
            try {
                fileChannel.truncate(size);
            } finally {
                rwLock.writeLock().unlock();
            }

            uploader.notifyUpdate();

            return this;
        }

        @Override
        public void sync() throws IOException {
            rwLock.writeLock().lock();
            try {
                fileChannel.force(true);
            } finally {
                rwLock.writeLock().unlock();
            }
        }
    }

}
