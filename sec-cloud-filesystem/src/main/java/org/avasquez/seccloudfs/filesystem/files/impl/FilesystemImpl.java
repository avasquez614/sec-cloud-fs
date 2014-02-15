package org.avasquez.seccloudfs.filesystem.files.impl;

import org.avasquez.seccloudfs.filesystem.files.File;
import org.avasquez.seccloudfs.filesystem.files.Filesystem;
import org.avasquez.seccloudfs.filesystem.files.User;
import org.springframework.beans.factory.annotation.Required;

import java.io.IOException;

/**
 * Created by alfonsovasquez on 01/02/14.
 */
public class FilesystemImpl implements Filesystem {

    private FileNodeStore fileNodeStore;
    private File root;

    @Required
    public void setFileNodeStore(FileNodeStore fileNodeStore) {
        this.fileNodeStore = fileNodeStore;
    }

    @Override
    public File createRoot(User owner, long permissions) throws IOException {
        root = fileNodeStore.create(true, owner, permissions);

        return root;
    }

    @Override
    public File getRoot() {
        return root;
    }

}
