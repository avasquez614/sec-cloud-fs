package org.avasquez.seccloudfs.filesystem.files;

import java.io.IOException;
import java.util.List;

/**
 * Created by alfonsovasquez on 13/01/14.
 */
public interface FileStore {

    File getRoot() throws IOException;

    File find(String id) throws IOException;

    List<File> findChildren(String id) throws IOException;

    File create(String parentId, String name, boolean dir) throws IOException;

    File rename(String id, String newName) throws IOException;

    File move(String id, String newParentId, String newName) throws IOException;

    void delete(String id) throws IOException;

}
