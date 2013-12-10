package org.avasquez.seccloudfs.secure.storage;

import java.io.DataInput;
import java.io.DataOutput;

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
     * @param dataInput the input where the data can be retrieved
     */
    void storeData(String id, DataInput dataInput);

    /**
     * Loads the data from the cloud.
     *
     * @param id            the ID used to identify the data
     * @param dataOutput    the output where the data can be written
     */
    void loadData(String id, DataOutput dataOutput);

    /**
     * Deletes the data.
     *
     * @param id    the ID used to identify the data.
     */
    void deleteData(String id);

}
