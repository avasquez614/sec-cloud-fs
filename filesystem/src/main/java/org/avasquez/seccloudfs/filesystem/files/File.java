package org.avasquez.seccloudfs.filesystem.files;

import org.avasquez.seccloudfs.filesystem.util.FlushableByteChannel;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by alfonsovasquez on 13/01/14.
 */
public interface File {

    /**
     * Returns the file's ID.
     */
    String getId();

    /**
     * Returns the name of the file.
     */
    String getName();

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
     * Returns the child with the specified name.
     */
    File getChild(String name) throws IOException;

    /**
     * Returns a collection with all the children (not necessarily sorted).
     */
    List<File> getChildren() throws IOException;

    /**
     * Returns a map where the keys are the children's names and the values are the children's IDs, or
     * null if the file is not a directory.
     */
    Map<String, String> getChildrenMap() throws IOException;

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
     *     <li>rename or move</li>
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
