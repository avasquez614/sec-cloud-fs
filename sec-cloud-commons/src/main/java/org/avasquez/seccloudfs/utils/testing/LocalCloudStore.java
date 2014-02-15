package org.avasquez.seccloudfs.utils.testing;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.springframework.beans.factory.annotation.Required;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Created by alfonsovasquez on 10/02/14.
 */
public class LocalCloudStore implements CloudStore {

    private Path storeDir;

    @Required
    public void setStoreDir(String storeDir) {
        this.storeDir = Paths.get(storeDir);
    }

    @Override
    public void upload(String dataId, ReadableByteChannel src, long length) throws IOException {
        Path dataFile = storeDir.resolve(dataId);

        try (FileChannel fileChannel = FileChannel.open(dataFile, StandardOpenOption.WRITE)) {
            fileChannel.transferFrom(src, 0, length);
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

        Files.delete(dataFile);
    }

}
