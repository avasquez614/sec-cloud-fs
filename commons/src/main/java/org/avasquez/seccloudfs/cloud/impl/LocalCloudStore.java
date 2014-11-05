package org.avasquez.seccloudfs.cloud.impl;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * {@link org.avasquez.seccloudfs.cloud.CloudStore} that uses the local filesystem to store the files.
 *
 * @author avasquez
 */
public class LocalCloudStore implements CloudStore {

    private static final Logger logger = LoggerFactory.getLogger(LocalCloudStore.class);

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
    public void setStoreDir(String storeDir) {
        this.storeDir = Paths.get(storeDir);
    }

    @PostConstruct
    public void init() throws IOException {
        if (!Files.exists(storeDir)) {
            logger.info("Creating local cloud store dir " + storeDir + "...");

            Files.createDirectories(storeDir);
        }
    }

    @Override
    public void upload(String id, ReadableByteChannel src, long length) throws IOException {
        Path path = getPath(id);

        try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            fileChannel.transferFrom(src, 0, length);
        }
    }

    @Override
    public void download(String id, WritableByteChannel target) throws IOException {
        Path path = getPath(id);

        try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ)) {
            fileChannel.transferTo(0, fileChannel.size(), target);
        }
    }

    @Override
    public void delete(String id) throws IOException {
        Files.delete(getPath(id));
    }

    private Path getPath(String filename) throws IOException {
        return storeDir.resolve(filename);
    }

}
