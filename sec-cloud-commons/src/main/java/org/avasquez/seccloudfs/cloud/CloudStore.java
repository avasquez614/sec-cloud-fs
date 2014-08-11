package org.avasquez.seccloudfs.cloud;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;

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
     * @param src       the source channel from where the data can be retrieved
     * @param length    the length of the data
     *
     * @return the final number of bytes uploaded
     */
    long upload(String id, SeekableByteChannel src, long length) throws IOException;

    /**
     * Downloads the data from the cloud.
     *
     * @param id        the ID used to identify the data
     * @param target    the target channel to where the data will be written
     *
     * @return the final number of bytes downloaded
     */
    long download(String id, SeekableByteChannel target) throws IOException;

    /**
     * Deletes the data.
     *
     * @param id the ID used to identify the data.
     */
    void delete(String id) throws IOException;

    /**
     * Returns the total space, in bytes, of the cloud store.
     *
     * @return the size of the cloud store, in bytes
     */
    long getTotalSpace() throws IOException;

    /**
     * Returns the amount space, in bytes, that can be used for storage.
     *
     * @return the number of bytes available
     */
    long getAvailableSpace() throws IOException;

}
