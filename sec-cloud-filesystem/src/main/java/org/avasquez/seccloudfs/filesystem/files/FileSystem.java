package org.avasquez.seccloudfs.filesystem.files;

import java.io.IOException;

/**
 * Created by alfonsovasquez on 01/02/14.
 */
public interface FileSystem {

    File getRoot() throws IOException;

    File createRoot(User owner, long permissions) throws IOException;

    long getTotalSpace() throws IOException;

    long getAvailableSpace() throws IOException;

    long getTotalFiles() throws IOException;

}
