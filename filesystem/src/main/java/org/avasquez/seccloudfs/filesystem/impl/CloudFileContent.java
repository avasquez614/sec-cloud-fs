package org.avasquez.seccloudfs.filesystem.impl;

import org.avasquez.seccloudfs.filesystem.FileContent;
import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;
import org.avasquez.seccloudfs.secure.storage.SecureCloudStorage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
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
    private FileChannel content;
    private SecureCloudStorage cloudStorage;
    private FileUploader fileUploader;

    public CloudFileContent(FileMetadata metadata, FileChannel content, SecureCloudStorage cloudStorage,
                            FileUploader fileUploader) {
        this.metadata = metadata;
        this.content = content;
        this.cloudStorage = cloudStorage;
        this.fileUploader = fileUploader;
    }

    @Override
    public long getPosition() throws IOException {
        return content.position();
    }

    @Override
    public void setPosition(long position) throws IOException {
        content.position(position);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        int length = dst.capacity();
        long position = getPosition();

        downloadChunks(getChunksToDownload(position, length));

        return content.read(dst, position);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        int length = src.capacity();
        long position = getPosition();

        downloadChunks(getChunksToDownload(position, length));

        int written = content.write(src);
        updateMetadataOnWrite(position, written);

        fileUploader.upload(new FileUpdate(position, written));

        return written;
    }

    @Override
    public int read(ByteBuffer dst, long position) throws IOException {
        int length = dst.capacity();

        downloadChunks(getChunksToDownload(position, length));

        return content.read(dst, position);
    }

    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        int length = src.capacity();

        downloadChunks(getChunksToDownload(position, length));

        int written = content.write(src, position);
        updateMetadataOnWrite(position, written);

        fileUploader.upload(new FileUpdate(position, written));

        return written;
    }

    @Override
    public boolean isOpen() {
        return content.isOpen();
    }

    @Override
    public void close() throws IOException {
        content.close();
    }

    @Override
    public void downloadAll() throws IOException {
        downloadChunks(getChunksToDownload(0, metadata.getSize()));
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

                cloudStorage.loadData(metadata.getChunkName(i), content);

                metadata.getCachedChunks().set(i);
            }
        }
    }

    protected void updateMetadataOnWrite(long position, int length) throws IOException {
        long endPosition = position + length - 1;
        int startChunk = metadata.getChunkForPosition(position);
        int endChunk = metadata.getChunkForPosition(endPosition);

        metadata.getCachedChunks().set(startChunk, endChunk + 1);
        if (endPosition >= metadata.getSize()) {
            metadata.setSize(endPosition + 1);
        }
    }

}
