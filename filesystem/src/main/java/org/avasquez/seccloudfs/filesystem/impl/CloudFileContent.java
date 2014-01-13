package org.avasquez.seccloudfs.filesystem.impl;

import org.avasquez.seccloudfs.filesystem.FileContent;
import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;
import org.avasquez.seccloudfs.secure.storage.SecureCloudStore;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.BitSet;

/**
 * Content of a cloud file that's cached in the local filesystem and downloaded on demand (a chunk at a time,
 * depending on the chunk(s) that are need to be read from/written to). Also, on file writes, new
 * {@link org.avasquez.seccloudfs.filesystem.impl.FileUpdate} are created and added to the updates queue for
 * processing.
 *
 * @author avasquez
 */
public class CloudFileContent implements FileContent {

    private FileMetadata metadata;
    private Path path;
    private SecureCloudStore cloudStorage;
    private CloudStorageUpdater cloudStorageUpdater;
    private FileChannel content;

    public CloudFileContent(FileMetadata metadata, Path path, SecureCloudStore cloudStorage,
                            CloudStorageUpdater cloudStorageUpdater) {
        this.metadata = metadata;
        this.path = path;
        this.cloudStorage = cloudStorage;
        this.cloudStorageUpdater = cloudStorageUpdater;
    }

    @Override
    public long getPosition() throws IOException {
        return getContent().position();
    }

    @Override
    public void setPosition(long position) throws IOException {
        getContent().position(position);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        int length = dst.capacity();
        long position = getPosition();

        downloadChunks(getChunksToDownload(position, length));

        return getContent().read(dst, position);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        int length = src.capacity();
        long position = getPosition();

        downloadChunks(getChunksToDownload(position, length));

        int written = getContent().write(src);

        updateFileMetadataOnWrite(position, written);

        cloudStorageUpdater.addUpdate(new FileUpdate(position, written, false));

        return written;
    }

    @Override
    public int read(ByteBuffer dst, long position) throws IOException {
        int length = dst.capacity();

        downloadChunks(getChunksToDownload(position, length));

        return getContent().read(dst, position);
    }

    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        int length = src.capacity();

        downloadChunks(getChunksToDownload(position, length));

        int written = getContent().write(src, position);

        updateFileMetadataOnWrite(position, written);

        cloudStorageUpdater.addUpdate(new FileUpdate(position, written, false));

        return written;
    }

    @Override
    public void copyTo(FileContent target) throws IOException {
        if (!(target instanceof CloudFileContent)) {
            throw new IllegalArgumentException("Target argument should be an instance of " + CloudFileContent.class);
        }

        CloudFileContent targetContent = (CloudFileContent) target;

        downloadAllChunks();

        Files.copy(path, targetContent.path, StandardCopyOption.REPLACE_EXISTING);

        updateFileMetadataOnCopy(targetContent.metadata);

        targetContent.uploadAllChunks();
    }

    @Override
    public void truncate(long size) throws IOException {
        long oldSize = metadata.getSize();
        if (oldSize > size) {
            getContent().truncate(size);

            updateFileMetadataOnTruncate(oldSize, size);

            long position = size - 1;
            long length = oldSize - size;

            cloudStorageUpdater.addUpdate(new FileUpdate(position, length, true));
        }
    }

    @Override
    public void delete() throws IOException {
        long size = metadata.getSize();

        Files.deleteIfExists(path);

        updateFileMetadataOnDelete();

        cloudStorageUpdater.addUpdate(new FileUpdate(0, size, true));
    }

    @Override
    public boolean isOpen() {
        if (content != null) {
            return content.isOpen();
        } else {
            return false;
        }
    }

    @Override
    public void close() throws IOException {
        if (content != null) {
            content.close();
        }
    }

    protected FileChannel getContent() throws IOException {
        if (content == null) {
            content = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE);
        }

        return content;
    }

    protected BitSet getChunksToDownload(long position, long length) {
        long endPosition = position + length - 1;
        int startChunk = metadata.getChunkForPosition(position);
        int endChunk = metadata.getChunkForPosition(endPosition);
        BitSet cachedChunks = metadata.getCachedChunks();
        int currentNumChunks = cachedChunks.size();
        BitSet chunksToDownload = new BitSet(currentNumChunks);

        if (endChunk >= currentNumChunks) {
            endChunk = currentNumChunks - 1;
        }

        for (int i = startChunk; i <= endChunk; i++) {
            if (!cachedChunks.get(i)) {
                chunksToDownload.set(i);
            }
        }

        return chunksToDownload;
    }

    protected void downloadChunks(BitSet chunksToDownload) throws IOException {
        if (chunksToDownload.cardinality() > 0) {
            for (int i = chunksToDownload.nextSetBit(0); i >= 0; i = chunksToDownload.nextSetBit(i + 1)) {
                content.position(i * metadata.getChunkSize());

                cloudStorage.download(metadata.getChunkName(i), content);

                metadata.getCachedChunks().set(i);
            }
        }
    }

    protected void downloadAllChunks() throws IOException {
        downloadChunks(getChunksToDownload(0, metadata.getSize()));
    }

    protected void uploadAllChunks() throws IOException {
        cloudStorageUpdater.addUpdate(new FileUpdate(0, metadata.getSize(), false));
    }

    protected void updateFileMetadataOnWrite(long position, int length) {
        long endPosition = position + length - 1;
        int startChunk = metadata.getChunkForPosition(position);
        int endChunk = metadata.getChunkForPosition(endPosition);

        metadata.getCachedChunks().set(startChunk, endChunk + 1);
        if (endPosition >= metadata.getSize()) {
            metadata.setSize(endPosition + 1);
        }
    }

    protected void updateFileMetadataOnCopy(FileMetadata targetMetadata) {
        targetMetadata.setCachedChunks(metadata.getCachedChunks());
        targetMetadata.setSize(metadata.getSize());
    }

    protected void updateFileMetadataOnTruncate(long oldSize, long newSize) {
        int chunksToDelete = (int) (oldSize - newSize);
        BitSet oldCachedChunks = metadata.getCachedChunks();
        BitSet newCachedChunks = new BitSet(oldCachedChunks.size() - chunksToDelete);

        for (int i = 0; i < newCachedChunks.size(); i++) {
            newCachedChunks.set(i, oldCachedChunks.get(i));
        }

        metadata.setCachedChunks(newCachedChunks);
        metadata.setSize(newSize);
    }

    protected void updateFileMetadataOnDelete() {
        metadata.setCachedChunks(new BitSet());
        metadata.setSize(0);
    }

}
