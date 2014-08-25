package org.avasquez.seccloudfs.filesystem.files.impl;

import java.io.IOException;

import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.filesystem.content.ContentStore;
import org.avasquez.seccloudfs.filesystem.db.model.FileSystemInfo;
import org.avasquez.seccloudfs.filesystem.db.repos.FileSystemInfoRepository;
import org.avasquez.seccloudfs.filesystem.files.File;
import org.avasquez.seccloudfs.filesystem.files.FileSystem;
import org.avasquez.seccloudfs.filesystem.files.User;
import org.springframework.beans.factory.annotation.Required;

/**
 * Created by alfonsovasquez on 01/02/14.
 */
public class FileSystemImpl implements FileSystem {

    private FileSystemInfoRepository fileSystemInfoRepo;
    private FileObjectStore fileObjectStore;
    private ContentStore contentStore;

    private FileObject root;

    @Required
    public void setFileSystemInfoRepo(FileSystemInfoRepository fileSystemInfoRepo) {
        this.fileSystemInfoRepo = fileSystemInfoRepo;
    }

    @Required
    public void setFileObjectStore(FileObjectStore fileObjectStore) {
        this.fileObjectStore = fileObjectStore;
    }

    @Required
    public void setContentStore(ContentStore contentStore) {
        this.contentStore = contentStore;
    }

    @Override
    public File getRoot() throws IOException {
        if (root == null) {
            FileSystemInfo info;
            try {
                info = fileSystemInfoRepo.getSingleton();
            } catch (DbException e) {
                throw new IOException("Unable to retrieve singleton filesystem info from DB", e);
            }

            if (info != null) {
                root = fileObjectStore.find(info.getRootDirectory());
            }
        }

        return root;
    }

    @Override
    public File createRoot(User owner, long permissions) throws IOException {
        root = fileObjectStore.create(true, owner, permissions);

        FileSystemInfo info = new FileSystemInfo(root.getId());
        try {
            fileSystemInfoRepo.insert(info);
        } catch (DbException e) {
            throw new IOException("Unable to insert singleton" + info + " into DB", e);
        }

        return root;
    }

    @Override
    public long getTotalSpace() throws IOException {
        return contentStore.getTotalSpace();
    }

    @Override
    public long getAvailableSpace() throws IOException {
        return contentStore.getAvailableSpace();
    }

    @Override
    public long getTotalFiles() throws IOException {
        return fileObjectStore.getTotalFiles();
    }

}
