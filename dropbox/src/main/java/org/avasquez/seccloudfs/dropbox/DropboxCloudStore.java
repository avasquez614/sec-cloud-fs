package org.avasquez.seccloudfs.dropbox;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxStreamWriter;
import com.dropbox.core.DbxWriteMode;
import org.avasquez.seccloudfs.cloud.CloudStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Dropbox implementation of {@link org.avasquez.seccloudfs.cloud.CloudStore}.
 *
 * @author avasquez
 */
public class DropboxCloudStore implements CloudStore {

    private static final Logger logger = LoggerFactory.getLogger(DropboxCloudStore.class);

    private String name;
    private DbxClient client;
    private String rootFolderName;
    private long chunkedUploadThreshold;

    public DropboxCloudStore(String name, DbxClient client, String rootFolderName, long chunkedUploadThreshold) {
        this.name = name;
        this.client = client;
        this.rootFolderName = rootFolderName;
        this.chunkedUploadThreshold = chunkedUploadThreshold;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void upload(String id, ReadableByteChannel src, long length) throws IOException {
        String path = getPath(id);

        logger.debug("Started uploading {}/{}", name, id);

        try {
            InputStream content = Channels.newInputStream(src);
            DbxStreamWriter.InputStreamCopier streamWriter = new DbxStreamWriter.InputStreamCopier(content);

            if (length < chunkedUploadThreshold) {
                logger.debug("Using direct upload for {}/{}", name, id);

                client.uploadFile(path, DbxWriteMode.force(), length, streamWriter);
            } else {
                logger.debug("Using chunked upload for {}/{}", name, id);

                client.uploadFileChunked(path, DbxWriteMode.force(), length, streamWriter);
            }
        } catch (Exception e) {
            throw new IOException("Error uploading " + name + "/" + id, e);
        }

        logger.debug("Finished uploading {}/{}", name, id);
    }

    @Override
    public void download(String id, WritableByteChannel target) throws IOException {
        String path = getPath(id);

        logger.debug("Started downloading {}/{}", name, id);

        try {
            client.getFile(path, null, Channels.newOutputStream(target));
        } catch (Exception e) {
            throw new IOException("Error downloading " + name + "/" + id, e);
        }

        logger.debug("Finished downloading {}/{}", name, id);
    }

    @Override
    public void delete(String id) throws IOException {
        String path = getPath(id);

        logger.debug("Deleting {}/{}", name, id);

        try {
            client.delete(path);
        } catch (Exception e) {
            throw new IOException("Error deleting " + name + "/" + id, e);
        }
    }

    private String getPath(String filename) {
        return "/" + rootFolderName + "/" + filename;
    }

}
