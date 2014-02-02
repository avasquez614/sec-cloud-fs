package org.avasquez.seccloudfs.filesystem.files.impl;

import org.avasquez.seccloudfs.filesystem.files.User;

import java.io.IOException;

/**
 * Created by alfonsovasquez on 13/01/14.
 */
public interface FileNodeStore {

    FileNode find(String id) throws IOException;

    FileNode create(boolean dir, User owner, long permissions) throws IOException;

    void delete(FileNode file) throws IOException;

}
