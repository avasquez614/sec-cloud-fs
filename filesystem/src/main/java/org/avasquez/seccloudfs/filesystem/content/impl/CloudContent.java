package org.avasquez.seccloudfs.filesystem.content.impl;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.filesystem.content.Content;
import org.avasquez.seccloudfs.filesystem.db.dao.ContentMetadataDao;
import org.avasquez.seccloudfs.filesystem.db.model.ContentMetadata;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Created by alfonsovasquez on 11/01/14.
 */
public class CloudContent implements DeletableContent {

    private static final String TMP_PATH_SUFFIX = ".download";

    private ContentMetadata metadata;
    private ContentMetadataDao metadataDao;
    private Path path;
    private CloudStore cloudStore;
    private Uploader uploader;
    private ReadWriteLock rwLock;

    public CloudContent(ContentMetadata metadata, ContentMetadataDao metadataDao, Path path, CloudStore cloudStore,
                        Uploader uploader, ReadWriteLock rwLock) throws IOException {
        this.metadata = metadata;
        this.metadataDao = metadataDao;
        this.path = path;
        this.cloudStore = cloudStore;
        this.uploader = uploader;
        this.rwLock = rwLock;
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
    public SeekableByteChannel getByteChannel() throws IOException {
        checkDownloaded();

        return new ContentByteChannel();
    }

    @Override
    public void copyTo(Content target) throws IOException {
        if (!(target instanceof CloudContent)) {
            throw new IllegalArgumentException("Target must be of type " + CloudContent.class.getName());
        }

        CloudContent targetContent = (CloudContent) target;
        ReadWriteLock targetRwLock = targetContent.rwLock;

        checkDownloaded();

        rwLock.readLock().lock();
        try {
            targetRwLock.writeLock().lock();
            try {
                Files.copy(path, targetContent.path, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                targetRwLock.writeLock().unlock();
            }
        } finally {
            rwLock.readLock().unlock();
        }

        uploader.notifyUpdate();
    }

    @Override
    public void delete() throws IOException {
        metadata.setMarkedAsDeleted(true);
        metadataDao.update(metadata);

        rwLock.writeLock().lock();
        try {
            Files.deleteIfExists(path);
        } finally {
            rwLock.writeLock().unlock();
        }

        uploader.notifyUpdate();
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

    private class ContentByteChannel implements SeekableByteChannel {

        private FileChannel fileChannel;

        public ContentByteChannel() throws IOException {
            fileChannel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE);
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
            rwLock.readLock().lock();
            try {
                return fileChannel.size();
            } finally {
                rwLock.readLock().unlock();
            }
        }

        @Override
        public boolean isOpen() {
            return fileChannel.isOpen();
        }

        @Override
        public void close() throws IOException {
            fileChannel.close();
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

    }

}
