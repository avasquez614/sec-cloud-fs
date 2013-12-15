package org.avasquez.seccloudfs.filesystem.impl;

import org.avasquez.seccloudfs.filesystem.FileContent;
import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;
import org.avasquez.seccloudfs.secure.storage.SecureCloudStorage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.BitSet;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Content of a cloud file that's cached in the local filesystem and downloaded on demand (a chunk at a time,
 * depending on the chunk(s) that are need to be read from/written to).
 *
 * @author avasquez
 */
public class CloudFileContent implements FileContent {

    protected FileChannel content;
    protected FileMetadata metadata;
    protected SecureCloudStorage cloudStorage;
    protected ReadWriteLock readWriteLock;

    public CloudFileContent(FileChannel content, FileMetadata metadata, SecureCloudStorage cloudStorage,
                            ReadWriteLock readWriteLock) {
        this.content = content;
        this.metadata = metadata;
        this.cloudStorage = cloudStorage;
        this.readWriteLock = readWriteLock;
    }

    @Override
    public int read(ByteBuffer buffer, int position) throws IOException {
        int length = buffer.capacity();

        downloadChunks(getChunksToDownload(position, length));

        readWriteLock.readLock().lock();
        try {
            return content.read(buffer, position);
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    @Override
    public void write(ByteBuffer buffer, int position) throws IOException {
        int length = buffer.capacity();

        downloadChunks(getChunksToDownload(position, length));

        readWriteLock.writeLock().lock();
        try {
            content.write(buffer, position);

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

    protected void downloadChunks(BitSet chunksToDownload) throws IOException {
        if (chunksToDownload.cardinality() > 0) {
            readWriteLock.writeLock().lock();
            try {
                if (chunksToDownload.cardinality() > 0) {
                    for (int i = chunksToDownload.nextSetBit(0); i >= 0; i = chunksToDownload.nextSetBit(i + 1)) {
                        content.position(i * metadata.getChunkSize());

                        cloudStorage.loadData(metadata.getChunkName(i), content);

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
        metadata.setSize(content.size());
    }

}
