package org.avasquez.seccloudfs.cloud.impl;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.PostConstruct;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.utils.FileUtils;
import org.springframework.beans.factory.annotation.Required;

/**
 * Abstract {@link org.avasquez.seccloudfs.cloud.CloudStore} that other stores can extend so that a maximum store
 * size can be enforced.
 *
 * @author avasquez
 */
public abstract class MaxSizeAwareCloudStore implements CloudStore {

    protected long maxSize;
    protected AtomicLong currentSize;

    @Required
    public void setMaxSize(String maxSize) {
        this.maxSize = FileUtils.humanReadableByteSizeToByteCount(maxSize);
    }

    @PostConstruct
    public void init() throws IOException {
        this.currentSize = new AtomicLong(calculateCurrentSize());
    }

    @Override
    public long upload(String id, SeekableByteChannel src, long length) throws IOException {
        Object metadata = getMetadata(id);

        synchronized (this) {
            long dataSize = getDataSize(metadata);
            long delta = -dataSize + length;
            long newSize = currentSize.get() + delta;

            if (newSize > maxSize) {
                throw new IOException("Not enough space");
            } else {
                currentSize.addAndGet(delta);
            }
        }

        try {
            long bytesUploaded = doUpload(metadata, src, length);
            long delta = -length + bytesUploaded;

            currentSize.addAndGet(delta);

            return bytesUploaded;
        } catch (IOException e) {
            // Recalculate size since maybe some bytes were written
            currentSize = new AtomicLong(calculateCurrentSize());

            throw e;
        }
    }

    @Override
    public long download(String id, SeekableByteChannel target) throws IOException {
        return doDownload(getMetadata(id), target);
    }

    @Override
    public void delete(String id) throws IOException {
        Object metadata = getMetadata(id);
        long dataSize = getDataSize(metadata);

        doDelete(metadata);

        currentSize.addAndGet(-dataSize);
    }

    @Override
    public long getTotalSpace() throws IOException {
        return maxSize;
    }

    @Override
    public long getAvailableSpace() throws IOException {
        return maxSize - currentSize.get();
    }

    protected abstract Object getMetadata(String id) throws IOException;

    protected abstract long doUpload(Object metadata, SeekableByteChannel src, long length) throws IOException;

    protected abstract long doDownload(Object metadata, SeekableByteChannel target) throws IOException;

    protected abstract void doDelete(Object metadata) throws IOException;

    protected abstract long getDataSize(Object metadata) throws IOException;

    protected abstract long calculateCurrentSize() throws IOException;

}
