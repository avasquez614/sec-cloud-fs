package org.avasquez.seccloudfs.filesystem.dao;

import org.avasquez.seccloudfs.filesystem.File;

/**
 * Data Access Object for {@link org.avasquez.seccloudfs.filesystem.File}s.
 */
public interface FileDao {

    /**
     * Searches the file for the specified ID in the database.
     *
     * @param id    the ID of the file to look for
     *
     * @return the file, or null if not found
     */
    File findById(String id);

    /**
     * Saves the file in the database.
     *
     * @param file  the file to save
     */
    void save(File file);

    /**
     * Deletes the file for the specified ID in the database.
     *
     * @param id    the ID of the file to delete
     */
    void delete(String id);

}
