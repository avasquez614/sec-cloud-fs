package org.avasquez.seccloudfs.filesystem;

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
    File getParentDir();

    /**
     * Returns true if the file is a directory.
     */
    boolean isDirectory();

    /**
     * Returns the content of the file.
     */
    FileContent getContent();

    /**
     * Returns the date of the last access.
     */
    Date getLastAccess();

    /**
     * Returns the date of the last modification.
     */
    Date getLastModified();

}
