package org.avasquez.seccloudfs.filesystem.files;

import org.avasquez.seccloudfs.filesystem.util.FlushableByteChannel;

import java.io.IOException;
import java.util.Date;

/**
 * Created by alfonsovasquez on 13/01/14.
 */
public interface File {

    /**
     * Returns the file's ID.
     */
    String getId();

    /**
     * Returns the parent's ID.
     */
    String getParentId();

    /**
     * Returns the parent directory.
     */
    File getParent() throws IOException;

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
     * Returns the filename under the directory of the specified file ID, if the file is a child of
     * the directory, or null if it's not a child or if this is not a directory.
     */
    String getChildName(String fileId) throws IOException;

    /**
     * Returns true if the directory contains a child with the specified name.
     */
    boolean hasChild(String name) throws IOException;

    /**
     * Returns a list with the names of all the children (not necessarily sorted).
     */
    String[] getChildren() throws IOException;

    /**
     * Adds a child. If a child already exists for the specified name, it's replaced by the new one.
     *
     * @param name      the name of the child in the directory
     * @param fileId    the file ID of the child
     */
    void addChild(String name, String fileId) throws IOException;

    /**
     * Removes a child from the directory.
     *
     * @param name  the name of the child to remove
     */
    void removeChild(String name) throws IOException;

    /**
     * Removes a child from the directory.
     *
     * @param id    the ID of the child to remove
     */
    void removeChildById(String id) throws IOException;

    /**
     * Returns a byte channel that can be used to read/write to the content.
     */
    FlushableByteChannel getByteChannel() throws IOException;

    /**
     * Returns the time of the last of these changes:
     *
     * <ul>
     *     <li>ownership</li>
     *     <li>permissions</li>
     *     <li>content</li>
     * </ul>
     */
    Date getLastChangeTime();

    /**
     * Returns the time of the last access (read).
     */
    Date getLastAccessTime();

    /**
     * Returns the time of the last file modification (write).
     */
    Date getLastModifiedTime();

    /**
     * Sets the time of the last access (read).
     */
    void setLastAccessTime(Date lastAccessTime);

    /**
     * Sets the time of the last file modification (write).
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
     * Flushes any metadata changes to the underlying storage.
     */
    void flushMetadata() throws IOException;

}
