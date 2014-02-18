package org.avasquez.seccloudfs.utils.testing;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.utils.FileUtils;
import org.springframework.beans.factory.annotation.Required;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by alfonsovasquez on 10/02/14.
 */
public class LocalCloudStore implements CloudStore {

    private Path storeDir;
    private long maxDirSize;

    private AtomicLong currentDirSize;

    @Required
    public void setStoreDir(String storeDir) {
        this.storeDir = Paths.get(storeDir);
    }

    @Required
    public void setMaxDirSize(String maxDirSize) {
        this.maxDirSize = FileUtils.humanReadableByteSizeToByteCount(maxDirSize);
    }

    @PostConstruct
    public void init() throws IOException {
        currentDirSize = new AtomicLong(FileUtils.sizeOfDirectory(storeDir));
    }

    @Override
    public synchronized void upload(String dataId, ReadableByteChannel src, long length) throws IOException {
        Path dataFile = storeDir.resolve(dataId);

        long newDirSize;
        if (Files.exists(dataFile)) {
            newDirSize = currentDirSize.get() - Files.size(dataFile) + length;
        } else {
            newDirSize = currentDirSize.get() + length;
        }

        if (newDirSize > maxDirSize) {
            throw new IOException("Not enough space");
        }

        try (FileChannel fileChannel = FileChannel.open(dataFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            long bytesWritten = fileChannel.transferFrom(src, 0, length);

            currentDirSize.addAndGet(bytesWritten);
        }
    }

    @Override
    public void download(String dataId, WritableByteChannel target) throws IOException {
        Path dataFile = storeDir.resolve(dataId);

        try (FileChannel fileChannel = FileChannel.open(dataFile, StandardOpenOption.READ)) {
            fileChannel.transferTo(0, fileChannel.size(), target);
        }
    }

    @Override
    public void delete(String dataId) throws IOException {
        Path dataFile = storeDir.resolve(dataId);
        long size = Files.size(dataFile);

        Files.delete(dataFile);

        currentDirSize.addAndGet(-size);
    }

    @Override
    public long getTotalSpace() throws IOException {
        return maxDirSize;
    }

    @Override
    public long getAvailableSpace() throws IOException {
        return maxDirSize - currentDirSize.get();
    }

}
