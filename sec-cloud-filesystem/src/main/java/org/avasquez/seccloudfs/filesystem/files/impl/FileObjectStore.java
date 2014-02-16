package org.avasquez.seccloudfs.filesystem.files.impl;

import org.avasquez.seccloudfs.filesystem.files.User;

import java.io.IOException;

/**
 * Created by alfonsovasquez on 13/01/14.
 */
public interface FileObjectStore {

    FileObject find(String id) throws IOException;

    FileObject create(boolean dir, User owner, long permissions) throws IOException;

    void delete(FileObject file) throws IOException;

}
