package org.avasquez.seccloudfs.cloud.impl;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.avasquez.seccloudfs.utils.FileUtils;
import org.springframework.beans.factory.annotation.Required;

/**
 * {@link org.avasquez.seccloudfs.cloud.CloudStore} that uses the local filesystem to store the files.
 *
 * @author avasquez
 */
public class LocalCloudStore extends MaxSizeAwareCloudStore {

    private String name;
    private Path storeDir;

    @Override
    public String getName() {
        return name;
    }

    @Required
    public void setName(final String name) {
        this.name = name;
    }

    @Required
    public void setStoreDir(final Path storeDir) {
        this.storeDir = storeDir;
    }

    @Override
    protected Object getMetadata(final String id) throws IOException {
        return storeDir.resolve(id);
    }

    @Override
    protected long doUpload(Object metadata, SeekableByteChannel src, long length) throws IOException {
        Path path = (Path) metadata;

        try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            return fileChannel.transferFrom(src, 0, length);
        }
    }

    @Override
    protected long doDownload(Object metadata, SeekableByteChannel target) throws IOException {
        Path path = (Path) metadata;

        try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ)) {
            return fileChannel.transferTo(0, fileChannel.size(), target);
        }
    }

    @Override
    protected void doDelete(Object metadata) throws IOException {
        Files.delete((Path) metadata);
    }

    @Override
    protected long getDataSize(Object metadata) throws IOException {
        return Files.size((Path) metadata);
    }

    @Override
    protected long calculateCurrentSize() throws IOException {
        return FileUtils.sizeOfDirectory(storeDir);
    }

}
