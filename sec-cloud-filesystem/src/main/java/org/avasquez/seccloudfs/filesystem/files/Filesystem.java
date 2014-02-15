package org.avasquez.seccloudfs.filesystem.files;

import java.io.IOException;

/**
 * Created by alfonsovasquez on 01/02/14.
 */
public interface Filesystem {

    File createRoot(User owner, long permissions) throws IOException;

    File getRoot();

}
