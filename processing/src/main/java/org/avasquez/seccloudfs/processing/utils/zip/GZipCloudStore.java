package org.avasquez.seccloudfs.processing.utils.zip;

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
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

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
    public void upload(String id, ReadableByteChannel src, long length) throws IOException {
        Path tmpFile = Files.createTempFile(tmpDir, id, ".zipped");

        try (FileChannel tmpChannel = FileChannel.open(tmpFile, FileUtils.TMP_FILE_OPEN_OPTIONS)) {
            InputStream in = Channels.newInputStream(src);
            GZIPOutputStream out = new GZIPOutputStream(Channels.newOutputStream(tmpChannel));

            IOUtils.copyLarge(in, out);

            out.finish();

            logger.debug("Data '{}' successfully zipped", id);

            // Reset channel for reading
            tmpChannel.position(0);

            underlyingStore.upload(id, tmpChannel, tmpChannel.size());
        }
    }

    @Override
    public void download(String id, WritableByteChannel target) throws IOException {
        Path tmpFile = Files.createTempFile(tmpDir, id, ".unzipped");

        try (FileChannel tmpChannel = FileChannel.open(tmpFile, FileUtils.TMP_FILE_OPEN_OPTIONS)) {
            underlyingStore.download(id, tmpChannel);

            // Reset channel for reading
            tmpChannel.position(0);

            GZIPInputStream in = new GZIPInputStream(Channels.newInputStream(tmpChannel));
            OutputStream out = Channels.newOutputStream(target);

            IOUtils.copyLarge(in, out);

            logger.debug("Data '{}' successfully unzipped", id);
        }
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
