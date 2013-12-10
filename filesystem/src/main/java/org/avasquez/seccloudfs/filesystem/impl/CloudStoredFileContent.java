package org.avasquez.seccloudfs.filesystem.impl;

import org.avasquez.seccloudfs.filesystem.File;
import org.avasquez.seccloudfs.filesystem.FileContent;
import org.avasquez.seccloudfs.filesystem.dao.FileDao;
import org.avasquez.seccloudfs.secure.storage.SecureCloudStorage;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.BitSet;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * A file content that is stored securely in the cloud
 *
 * @author avasquez
 */
public class CloudStoredFileContent implements FileContent {

    protected RandomAccessFile content;
    protected File file;
    protected FileDao fileDao;
    protected SecureCloudStorage cloudStorage;
    protected ReadWriteLock readWriteLock;

    public CloudStoredFileContent(RandomAccessFile content, File file, FileDao fileDao,
                                  SecureCloudStorage cloudStorage, ReadWriteLock readWriteLock) {
        this.content = content;
        this.file = file;
        this.fileDao = fileDao;
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

            updateModifiedChunks(offset, length);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public void close() throws IOException {
        content.close();
    }

    protected BitSet getChunks(int offset, int length) {
        int endOffset = offset + length - 1;
        int startChunk = (int) (offset / file.getChunkSize());
        int endChunk = (int) (endOffset / file.getChunkSize());
        int currentNumChunks = file.getAvailableChunks().size();
        BitSet requiredChunks = new BitSet(currentNumChunks);

        for (int i = startChunk; i <= endChunk; i++) {
            if (i < currentNumChunks) {
                requiredChunks.set(i);
            }
        }

        return requiredChunks;
    }

    protected boolean areRequiredChunksAvailable(BitSet requiredChunks, BitSet availableChunks) {
        availableChunks = (BitSet) availableChunks.clone();
        availableChunks.and(requiredChunks);

        return availableChunks.equals(requiredChunks);
    }

    protected void downloadRequiredChunks(int offset, int length) throws IOException {
        BitSet requiredChunks = getChunks(offset, length);
        BitSet availableChunks = file.getAvailableChunks();

        if (!areRequiredChunksAvailable(requiredChunks, availableChunks)) {
            readWriteLock.writeLock().lock();
            try {
                if (!areRequiredChunksAvailable(requiredChunks, availableChunks)) {
                    for (int i = requiredChunks.nextSetBit(0); i >= 0; i = requiredChunks.nextSetBit(i + 1)) {
                        content.seek(i * file.getChunkSize());

                        cloudStorage.loadData(file.getChunkId(i), content);

                        file.getAvailableChunks().set(i);
                        fileDao.save(file);
                    }
                }
            } finally {
                readWriteLock.writeLock().unlock();
            }
        }
    }

    protected void updateModifiedChunks(int offset, int length) {
        BitSet modifiedChunks = getChunks(offset, length);
        BitSet newAvailableChunks = (BitSet) file.getAvailableChunks().clone();

        newAvailableChunks.or(modifiedChunks);

        if (!newAvailableChunks.equals(file.getAvailableChunks())) {
            file.setAvailableChunks(newAvailableChunks);
            fileDao.save(file);
        }
    }

}
