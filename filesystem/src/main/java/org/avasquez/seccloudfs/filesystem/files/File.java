package org.avasquez.seccloudfs.filesystem.files;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
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
    SeekableByteChannel getByteChannel() throws IOException;

    /**
     * Returns the time of the last access (read).
     */
    Date getLastAccessTime();

    /**
     * Returns the time of the last modification (write).
     */
    Date getLastModifiedTime();

}
