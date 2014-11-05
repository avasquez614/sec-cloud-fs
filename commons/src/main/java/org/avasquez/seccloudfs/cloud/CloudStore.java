package org.avasquez.seccloudfs.cloud;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Handles the storage of data in the cloud.
 *
 * @author avasquez
 */
public interface CloudStore {

    /**
     * Returns the name of this cloud store.
     */
    String getName();

    /**
     * Uploads the given data in the cloud.
     *
     * @param id        the ID used to identify the data
     * @param src       the source channel from where the data should be retrieved
     * @param length    the length of the data
     */
    void upload(String id, ReadableByteChannel src, long length) throws IOException;

    /**
     * Downloads the data from the cloud.
     *
     * @param id        the ID used to identify the data
     * @param target    the target channel where the data should be written to
     *
     * @return the final number of bytes downloaded
     */
    void download(String id, WritableByteChannel target) throws IOException;

    /**
     * Deletes the data.
     *
     * @param id the ID used to identify the data.
     */
    void delete(String id) throws IOException;

}
