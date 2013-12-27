package org.avasquez.seccloudfs.filesystem;

import java.io.IOException;
import java.util.Date;

/**
 * Representation of a file in the cloud virtual filesystem.
 */
public interface File {

    /**
     * Returns the name of the file.
     */
    String getName();

    /**
     * Returns the path of the file.
     */
    String getPath();

    /**
     * Returns the parent directory's path.
     */
    String getParent();

    /**
     * Returns the parent directory.
     */
    File getParentDir() throws IOException;

    /**
     * Returns true if the file is a directory.
     */
    boolean isDirectory();

    /**
     * Returns the content of the file.
     */
    FileContent getContent() throws IOException;

    /**
     * Returns the date of the last access (read).
     */
    Date getLastAccess();

    /**
     * Returns the date of the last modification (write).
     */
    Date getLastModified();

    /**
     * Sets the date of the last access (read).
     *
     * @param date  the last access date
     */
    void setLastAccess(Date date);

    /**
     * Sets the date of the last modification (write).
     *
     * @param date  the last modification date
     */
    void setLastModified(Date date);

}
