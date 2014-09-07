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
    public long upload(String id, ReadableByteChannel src, long length) throws IOException {
        Object dataObject = getDataObject(id, false);

        synchronized (this) {
            long dataSize = getDataSize(dataObject);
            long delta = -dataSize + length;
            long newSize = currentSize.get() + delta;

            if (newSize > maxSize) {
                throw new IOException("Not enough space");
            } else {
                currentSize.addAndGet(delta);
            }
        }

        try {
            long bytesUploaded = doUpload(id, dataObject, src, length);
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
    public long download(String id, WritableByteChannel target) throws IOException {
        return doDownload(id, getDataObject(id, true), target);
    }

    @Override
    public void delete(String id) throws IOException {
        Object metadata = getDataObject(id, false);
        long dataSize = getDataSize(metadata);

        doDelete(id, metadata);

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

    protected abstract Object getDataObject(String id, boolean withData) throws IOException;

    protected abstract long doUpload(String id, Object dataObject, ReadableByteChannel src,
                                     long length) throws IOException;

    protected abstract long doDownload(String id, Object dataObject, WritableByteChannel target) throws IOException;

    protected abstract void doDelete(String id, Object dataObject) throws IOException;

    protected abstract long getDataSize(Object dataObject) throws IOException;

    protected abstract long calculateCurrentSize() throws IOException;

}
