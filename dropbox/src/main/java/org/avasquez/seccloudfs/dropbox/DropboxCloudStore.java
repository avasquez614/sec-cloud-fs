package org.avasquez.seccloudfs.dropbox;

import com.dropbox.core.DbxAccountInfo;
import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;
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
    public long upload(String id, ReadableByteChannel src, long length) throws IOException {
        DbxEntry.File file;

        logger.debug("Uploading data {}/{}", name, id);

        try {
            file = client.uploadFile(id, DbxWriteMode.force(), length, Channels.newInputStream(src));
        } catch (Exception e) {
            throw new IOException("Error uploading data " + name + "/" + id, e);
        }

        return file.numBytes;
    }

    @Override
    public long download(String id, WritableByteChannel target) throws IOException {
        DbxEntry.File file;

        logger.debug("Downloading data {}/{}", name, id);

        try {
            file = client.getFile(id, null, Channels.newOutputStream(target));
        } catch (Exception e) {
            throw new IOException("Error downloading data " + name + "/" + id, e);
        }

        return file.numBytes;
    }

    @Override
    public void delete(String id) throws IOException {
        logger.debug("Deleting data {}/{}", name, id);

        try {
            client.delete(id);
        } catch (Exception e) {
            throw new IOException("Error deleting data " + name + "/" + id, e);
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
