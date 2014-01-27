package org.avasquez.seccloudfs.filesystem.files;

import java.io.IOException;

/**
 * Created by alfonsovasquez on 13/01/14.
 */
public interface FileStore {

    File getRoot() throws IOException;

    File find(String id) throws IOException;

    File create(File parent, String name, boolean dir, User owner, long permissions) throws IOException;

    File move(File file, File newParent, String newName) throws IOException;

    void delete(File file) throws IOException;

}
