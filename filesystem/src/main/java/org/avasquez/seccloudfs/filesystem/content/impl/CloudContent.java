package org.avasquez.seccloudfs.filesystem.content.impl;

import org.avasquez.seccloudfs.filesystem.content.Content;
import org.avasquez.seccloudfs.filesystem.db.model.ContentMetadata;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Created by alfonsovasquez on 11/01/14.
 */
public class CloudContent implements Content {

    private ContentMetadata metadata;
    private Path path;
    private Downloader downloader;
    private Uploader uploader;
    private ReadWriteLock readWriteLock;

    public CloudContent(ContentMetadata metadata, Path path, Downloader downloader, Uploader uploader,
                        ReadWriteLock readWriteLock) {
        this.metadata = metadata;
        this.path = path;
        this.downloader = downloader;
        this.uploader = uploader;
        this.readWriteLock = readWriteLock;
    }

    @Override
    public String getId() {
        return metadata.getId();
    }

    @Override
    public long getSize() {
        return metadata.getSize();
    }

    @Override
    public SeekableByteChannel getByteChannel() throws IOException {
        checkContentDownloaded();

        return new ContentByteChannel();
    }

    @Override
    public void copyFrom(Content srcContent) throws IOException {
        if (!(srcContent instanceof CloudContent)) {
            throw new IllegalArgumentException("Can't copy from " + srcContent + ". It must be of type" +
                    CloudContent.class.getName());
        }

        CloudContent srcCloudContent = (CloudContent) srcContent;
        Path srcPath = srcCloudContent.path;
        ReadWriteLock srcReadWriteLock = srcCloudContent.readWriteLock;

        srcCloudContent.checkContentDownloaded();

        srcReadWriteLock.readLock().lock();
        try {
            readWriteLock.writeLock().lock();
            try {
                Files.copy(srcPath, path, StandardCopyOption.REPLACE_EXISTING);

                metadata.setSize(Files.size(path));

                uploader.notifyUpdate();
            } finally {
                readWriteLock.writeLock().unlock();
            }
        } finally {
            srcReadWriteLock.readLock().unlock();
        }
    }

    private void checkContentDownloaded() throws IOException {
        if (getSize() > 0 && !Files.exists(path)) {
            synchronized (this) {
                if (!downloader.isContentDownloading()) {
                    try {
                        downloader.download();
                    } catch (IOException e) {
                        throw new IOException("Error while trying to download content '" + metadata.getId() + "'", e);
                    }
                } else {
                    downloader.awaitTillDownloadFinished();
                }
            }
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
            return fileChannel.size();
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
            readWriteLock.readLock().lock();
            try {
                return fileChannel.read(dst);
            } finally {
                readWriteLock.readLock().unlock();
            }
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            readWriteLock.writeLock().lock();
            try {
                int bytesWritten = fileChannel.write(src);

                metadata.setSize(fileChannel.size());

                uploader.notifyUpdate();

                return bytesWritten;
            } finally {
                readWriteLock.writeLock().unlock();
            }
        }

        @Override
        public SeekableByteChannel truncate(long size) throws IOException {
            readWriteLock.writeLock().lock();
            try {
                fileChannel.truncate(size);

                metadata.setSize(size);

                uploader.notifyUpdate();

                return this;
            } finally {
                readWriteLock.writeLock().unlock();
            }
        }

    }

}
