package org.avasquez.seccloudfs.filesystem.files.impl;

import java.io.IOException;

import org.avasquez.seccloudfs.filesystem.files.User;

/**
 * Created by alfonsovasquez on 13/01/14.
 */
public interface FileObjectStore {

    long getTotalFiles() throws IOException;

    FileObject find(String id) throws IOException;

    FileObject create(boolean dir, User owner, long permissions) throws IOException;

    void delete(FileObject file) throws IOException;

}
