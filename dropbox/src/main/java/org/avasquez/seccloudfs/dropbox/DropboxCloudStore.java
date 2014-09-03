package org.avasquez.seccloudfs.dropbox;

import com.dropbox.core.DbxAccountInfo;
import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxWriteMode;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;

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
    public long upload(String id, SeekableByteChannel src, long length) throws IOException {
        DbxEntry.File file;

        logger.debug("Uploading data '{}' to store {}", id, name);

        try {
            file = client.uploadFile(id, DbxWriteMode.force(), length, Channels.newInputStream(src));
        } catch (Exception e) {
            throw new IOException("Error uploading data '" + id + "' to store " + name, e);
        }

        return file.numBytes;
    }

    @Override
    public long download(String id, SeekableByteChannel target) throws IOException {
        DbxEntry.File file;

        logger.debug("Downloading data '{}' from store {}", id, name);

        try {
            file = client.getFile(id, null, Channels.newOutputStream(target));
        } catch (Exception e) {
            throw new IOException("Error downloading data '" + id + "' from store " + name, e);
        }

        return file.numBytes;
    }

    @Override
    public void delete(String id) throws IOException {
        logger.debug("Deleting data '{}' from store {}", id, name);

        try {
            client.delete(id);
        } catch (Exception e) {
            throw new IOException("Error deleting data '" + id + "' from store " + name, e);
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
        } catch (DbxException e) {
            throw new IOException("Error retrieving Dropbox account info for store " + name, e);
        }
    }

}