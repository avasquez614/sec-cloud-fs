package org.avasquez.seccloudfs.cloud.impl;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
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
    public void upload(String id, ReadableByteChannel src, long length) throws IOException {
        Object metadata = getMetadata(id);

        synchronized (this) {
            long size = getDataSize(metadata);
            long delta = -size + length;
            long newSize = currentSize.get() + delta;

            if (newSize > maxSize) {
                throw new IOException("Not enough space");
            } else {
                currentSize.addAndGet(delta);
            }
        }

        try {
            doUpload(id, metadata, src, length);
        } catch (IOException e) {
            // Recalculate size since maybe some bytes were written
            synchronized (this) {
                currentSize = new AtomicLong(calculateCurrentSize());
            }

            throw e;
        }
    }

    @Override
    public void download(String id, WritableByteChannel target) throws IOException {
        doDownload(id, getMetadata(id), target);
    }

    @Override
    public void delete(String id) throws IOException {
        Object metadata = getMetadata(id);
        long size = getDataSize(metadata);

        doDelete(id, metadata);

        currentSize.addAndGet(-size);
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

    protected abstract void doUpload(String id, Object metadata, ReadableByteChannel src,
                                     long length) throws IOException;

    protected abstract void doDownload(String id, Object metadata, WritableByteChannel target) throws IOException;

    protected abstract void doDelete(String id, Object metadata) throws IOException;

    protected abstract long getDataSize(Object metadata) throws IOException;

    protected abstract long calculateCurrentSize() throws IOException;

}
