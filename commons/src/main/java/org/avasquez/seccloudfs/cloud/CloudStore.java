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
     * Uploads the given data in the cloud.
     *
     * @param dataId    the ID used to identify the data
     * @param input     the input where the data can be retrieved
     * @param length    the length of the data
     */
    void upload(String dataId, ReadableByteChannel input, long length) throws IOException;

    /**
     * Downloads the data from the cloud.
     *
     * @param dataId        the ID used to identify the data
     * @param output    the output where the data can be written
     */
    void download(String dataId, WritableByteChannel output) throws IOException;

    /**
     * Deletes the data.
     *
     * @param dataId    the ID used to identify the data.
     */
    void delete(String dataId) throws IOException;

}
