package org.avasquez.seccloudfs.filesystem.files;

import org.avasquez.seccloudfs.filesystem.util.FlushableByteChannel;

import java.io.IOException;
import java.util.Date;

/**
 * Created by alfonsovasquez on 13/01/14.
 */
public interface File {

    /**
     * Returns the file's size.
     */
    long getSize() throws IOException;

    /**
     * Returns true if the file is a directory.
     */
    boolean isDirectory();

    /**
     * Returns true if the directory is empty.
     */
    boolean isEmpty() throws IOException;

    /**
     * Returns the child with the specified name.
     */
    File getChild(String name) throws IOException;

    /**
     * Returns true if the directory contains a child with the specified name.
     */
    boolean hasChild(String name) throws IOException;

    /**
     * Returns a list with the names of all the children (not necessarily sorted).
     */
    String[] getChildren() throws IOException;

    /**
     * Creates a new file under this directory
     *
     * @param name          the file's name
     * @param dir           if the file should be a directory
     * @param owner         the file's owner
     * @param permissions   the file's permissions
     *
     * @return the created file
     */
    File createFile(String name, boolean dir, User owner, long permissions) throws IOException;

    /**
     * Moves the child of the specified name under the specified directory with the new name.
     *
     * @param name      the child's name
     * @param newParent the new parent
     * @param newName   the new name
     */
    File moveFileTo(String name, File newParent, String newName) throws IOException;

    /**
     * Deletes the file with the specified name under this directory.
     *
     * @param name  the child's name
     */
    void delete(String name) throws IOException;

    /**
     * Returns a byte channel that can be used to read/write to the content.
     */
    FlushableByteChannel getByteChannel() throws IOException;

    /**
     * Returns the time of the last metadata or content (in case of dir, it's children) changes.
     */
    Date getLastChangeTime();

    /**
     * Returns the time of the last access (read or dir get children).
     */
    Date getLastAccessTime();

    /**
     * Returns the time of the last file modification (write or dir add/remove).
     */
    Date getLastModifiedTime();

    /**
     * Returns the time of the last metadata or content (in case of dir, it's children) changes.
     */
    void setLastChangeTime(Date lastChangeTime);

    /**
     * Sets the time of the last access (read or dir get children).
     */
    void setLastAccessTime(Date lastAccessTime);

    /**
     * Sets the time of the last file modification (write or dir add/remove).
     */
    void setLastModifiedTime(Date lastModifiedTime);

    /**
     * Returns the file's owner.
     */
    User getOwner();

    /**
     * Sets the file's owner.
     *
     * @param fileOwner  the new owner.
     */
    void setOwner(User fileOwner);

    /**
     * Returns the file's permissions.
     */
    long getPermissions();

    /**
     * Sets the file's permissions.
     *
     * @param permissions   the new permissions
     */
    void setPermissions(long permissions);

    /**
     * Sync any metadata changes to the underlying storage.
     */
    void syncMetadata() throws IOException;

}
