package org.avasquez.seccloudfs.filesystem.impl;

import org.avasquez.seccloudfs.filesystem.FileContent;
import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;
import org.avasquez.seccloudfs.secure.storage.SecureCloudStorage;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.BitSet;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Content of a cloud file that's cached in the local filesystem and downloaded on demand (a chunk at a time,
 * depending on the chunk(s) that are need to be read from/written to).
 *
 * @author avasquez
 */
public class CachedCloudFileContent implements FileContent {

    protected RandomAccessFile content;
    protected FileMetadata metadata;
    protected SecureCloudStorage cloudStorage;
    protected ReadWriteLock readWriteLock;

    public CachedCloudFileContent(RandomAccessFile content, FileMetadata metadata, SecureCloudStorage cloudStorage,
                                  ReadWriteLock readWriteLock) {
        this.content = content;
        this.metadata = metadata;
        this.cloudStorage = cloudStorage;
        this.readWriteLock = readWriteLock;
    }

    @Override
    public int read(byte[] bytes, int position) throws IOException {
        int length = bytes.length;

        downloadRequiredChunks(position, length);

        readWriteLock.readLock().lock();
        try {
            content.seek(position);

            return content.read(bytes, position, length);
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    @Override
    public void write(byte[] bytes, int position) throws IOException {
        int length = bytes.length;

        downloadRequiredChunks(position, length);

        readWriteLock.writeLock().lock();
        try {
            content.seek(position);
            content.write(bytes, position, length);

            updateMetadataOnWrite(position, length);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public void close() throws IOException {
        content.close();
    }

    protected BitSet getChunksToDownload(int position, int length) {
        int endPosition = position + length - 1;
        int startChunk = (int) (position / metadata.getChunkSize());
        int endChunk = (int) (endPosition / metadata.getChunkSize());
        BitSet availableChunks = metadata.getCachedChunks();
        int currentNumChunks = availableChunks.size();
        BitSet chunksToDownload = new BitSet(currentNumChunks);

        if (endChunk >= currentNumChunks) {
            endChunk = currentNumChunks - 1;
        }

        for (int i = startChunk; i <= endChunk; i++) {
            if (!availableChunks.get(i)) {
                chunksToDownload.set(i);
            }
        }

        return chunksToDownload;
    }

    protected void downloadRequiredChunks(int position, int length) throws IOException {
        BitSet chunksToDownload = getChunksToDownload(position, length);

        if (chunksToDownload.cardinality() > 0) {
            readWriteLock.writeLock().lock();
            try {
                if (chunksToDownload.cardinality() > 0) {
                    for (int i = chunksToDownload.nextSetBit(0); i >= 0; i = chunksToDownload.nextSetBit(i + 1)) {
                        content.seek(i * metadata.getChunkSize());

                        cloudStorage.loadData(metadata.getChunkId(i), content);

                        metadata.getCachedChunks().set(i);
                    }
                }
            } finally {
                readWriteLock.writeLock().unlock();
            }
        }
    }

    protected void updateMetadataOnWrite(int position, int length) throws IOException {
        int endPosition = position + length - 1;
        int startChunk = (int) (position / metadata.getChunkSize());
        int endChunk = (int) (endPosition / metadata.getChunkSize());

        metadata.getCachedChunks().set(startChunk, endChunk + 1);
        metadata.setSize(content.length());
    }

}
