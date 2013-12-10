package org.avasquez.seccloudfs.filesystem.impl;

import org.avasquez.seccloudfs.filesystem.File;
import org.avasquez.seccloudfs.filesystem.FileContent;
import org.avasquez.seccloudfs.filesystem.dao.FileDao;
import org.avasquez.seccloudfs.secure.storage.SecureCloudStorage;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.BitSet;
import java.util.Date;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * A file content that is stored securely in the cloud.
 *
 * @author avasquez
 */
public class CloudStoredFileContent implements FileContent {

    protected RandomAccessFile content;
    protected File file;
    protected SecureCloudStorage cloudStorage;
    protected ReadWriteLock readWriteLock;

    public CloudStoredFileContent(RandomAccessFile content, File file, SecureCloudStorage cloudStorage,
                                  ReadWriteLock readWriteLock) {
        this.content = content;
        this.file = file;
        this.cloudStorage = cloudStorage;
        this.readWriteLock = readWriteLock;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
        downloadRequiredChunks(offset, length);

        readWriteLock.readLock().lock();
        try {
            return content.read(bytes, offset, length);
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
        downloadRequiredChunks(offset, length);

        readWriteLock.writeLock().lock();
        try {
            content.write(bytes, offset, length);

            updateMetadataOnWrite(offset, length);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public void close() throws IOException {
        content.close();
    }

    protected BitSet getChunksToDownload(int offset, int length) {
        int endOffset = offset + length - 1;
        int startChunk = (int) (offset / file.getChunkSize());
        int endChunk = (int) (endOffset / file.getChunkSize());
        BitSet availableChunks = file.getAvailableChunks();
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

    protected void downloadRequiredChunks(int offset, int length) throws IOException {
        BitSet chunksToDownload = getChunksToDownload(offset, length);

        if (chunksToDownload.cardinality() > 0) {
            readWriteLock.writeLock().lock();
            try {
                if (chunksToDownload.cardinality() > 0) {
                    for (int i = chunksToDownload.nextSetBit(0); i >= 0; i = chunksToDownload.nextSetBit(i + 1)) {
                        content.seek(i * file.getChunkSize());

                        cloudStorage.loadData(file.getChunkId(i), content);

                        file.getAvailableChunks().set(i);
                    }
                }
            } finally {
                readWriteLock.writeLock().unlock();
            }
        }
    }

    protected void updateMetadataOnWrite(int offset, int length) throws IOException {
        int endOffset = offset + length - 1;
        int startChunk = (int) (offset / file.getChunkSize());
        int endChunk = (int) (endOffset / file.getChunkSize());

        file.getAvailableChunks().set(startChunk, endChunk + 1);
        file.setSize(content.length());
    }

}
