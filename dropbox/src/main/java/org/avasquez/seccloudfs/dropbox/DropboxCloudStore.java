package org.avasquez.seccloudfs.dropbox;

import com.dropbox.core.DbxAccountInfo;
import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxWriteMode;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dropbox implementation of {@link org.avasquez.seccloudfs.cloud.CloudStore}.
 *
 * @author avasquez
 */
public class DropboxCloudStore implements CloudStore {

    private static final Logger logger = LoggerFactory.getLogger(DropboxCloudStore.class);

    private String name;
    private DbxClient client;

    public DropboxCloudStore(String name, DbxClient client) {
        this.name = name;
        this.client = client;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void upload(String filename, ReadableByteChannel src, long length) throws IOException {
        logger.debug("Started uploading {}/{}", name, filename);

        try {
            client.uploadFile("/" + filename, DbxWriteMode.force(), length, Channels.newInputStream(src));
        } catch (Exception e) {
            throw new IOException("Error uploading " + name + "/" + filename, e);
        }

        logger.debug("Finished uploading {}/{}", name, filename);
    }

    @Override
    public void download(String filename, WritableByteChannel target) throws IOException {
        logger.debug("Started downloading {}/{}", name, filename);

        try {
            client.getFile("/" + filename, null, Channels.newOutputStream(target));
        } catch (Exception e) {
            throw new IOException("Error downloading " + name + "/" + filename, e);
        }

        logger.debug("Finished downloading {}/{}", name, filename);
    }

    @Override
    public void delete(String filename) throws IOException {
        logger.debug("Deleting {}/{}", name, filename);

        try {
            client.delete("/" + filename);
        } catch (Exception e) {
            throw new IOException("Error deleting " + name + "/" + filename, e);
        }
    }

    @Override
    public long getTotalSpace() throws IOException {
        return getQuota().total;
    }

    @Override
    public long getAvailableSpace() throws IOException {
        DbxAccountInfo.Quota quota = getQuota();

        return quota.total - quota.normal - quota.shared;
    }

    private DbxAccountInfo.Quota getQuota() throws IOException {
        try {
            return client.getAccountInfo().quota;
        } catch (Exception e) {
            throw new IOException("Error retrieving Dropbox account info for store " + name, e);
        }
    }

}
