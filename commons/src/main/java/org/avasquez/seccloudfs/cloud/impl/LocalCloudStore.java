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
    public void setName(String name) {
        this.name = name;
    }

    @Required
    public void setStoreDir(Path storeDir) {
        this.storeDir = storeDir;
    }

    @Override
    protected Object getDataObject(String id, boolean withData) throws IOException {
        return storeDir.resolve(id);
    }

    @Override
    protected long doUpload(String id, Object dataObject, SeekableByteChannel src, long length) throws IOException {
        Path path = (Path) dataObject;

        try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            return fileChannel.transferFrom(src, 0, length);
        }
    }

    @Override
    protected long doDownload(String id, Object dataObject, SeekableByteChannel target) throws IOException {
        Path path = (Path) dataObject;

        try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ)) {
            return fileChannel.transferTo(0, fileChannel.size(), target);
        }
    }

    @Override
    protected void doDelete(String id, Object dataObject) throws IOException {
        Files.delete((Path) dataObject);
    }

    @Override
    protected long getDataSize(Object dataObject) throws IOException {
        return Files.size((Path) dataObject);
    }

    @Override
    protected long calculateCurrentSize() throws IOException {
        return FileUtils.sizeOfDirectory(storeDir);
    }

}
