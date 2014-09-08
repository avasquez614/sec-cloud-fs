package org.avasquez.seccloudfs.processing.utils.zip;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * {@link org.avasquez.seccloudfs.cloud.CloudStore} decorator that zips the data before upload and unzips it
 * after download. For zipping the GZIP format is used.
 *
 * @author avasquez
 */
public class GZipCloudStore implements CloudStore {

    private static final Logger logger = LoggerFactory.getLogger(GZipCloudStore.class);

    private CloudStore underlyingStore;
    private Path tmpDir;

    @Required
    public void setUnderlyingStore(CloudStore underlyingStore) {
        this.underlyingStore = underlyingStore;
    }

    @Required
    public void setTmpDir(String tmpDirPath) {
        tmpDir = Paths.get(tmpDirPath);
    }

    @Override
    public String getName() {
        return underlyingStore.getName();
    }

    @Override
    public long upload(String id, ReadableByteChannel src, long length) throws IOException {
        Path tmpFile = Files.createTempFile(tmpDir, id, null, null);

        try (FileChannel tmpChannel = FileChannel.open(tmpFile, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            InputStream in = Channels.newInputStream(src);
            OutputStream out = Channels.newOutputStream(tmpChannel);



            logger.debug("Data '{}' successfully zipped", id);

            // Reset channel for reading
            tmpChannel.position(0);

            return underlyingStore.upload(id, tmpChannel, length);
        }
    }

    @Override
    public long download(String id, WritableByteChannel target) throws IOException {
        return 0;
    }

    @Override
    public void delete(String id) throws IOException {

    }

    @Override
    public long getTotalSpace() throws IOException {
        return underlyingStore.getTotalSpace();
    }

    @Override
    public long getAvailableSpace() throws IOException {
        return underlyingStore.getAvailableSpace();
    }

}
