package org.avasquez.seccloudfs.secure.storage;

import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Handles the secure storage of data in the cloud by applying protection mechanisms like IDA and encryption.
 *
 * @author avasquez
 */
public interface SecureCloudStorage {

    /**
     * Stores the given data securely in the cloud.
     *
     * @param id        the ID used to identify the data
     * @param input     the input where the data can be retrieved
     * @param length    the length of the data
     */
    void storeData(String id, ReadableByteChannel input, long length);

    /**
     * Loads the data from the cloud.
     *
     * @param id        the ID used to identify the data
     * @param output    the output where the data can be written
     */
    void loadData(String id, WritableByteChannel output);

    /**
     * Deletes the data.
     *
     * @param id    the ID used to identify the data.
     */
    void deleteData(String id);

}
