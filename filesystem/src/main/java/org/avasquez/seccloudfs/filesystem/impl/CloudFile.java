package org.avasquez.seccloudfs.filesystem.impl;

import org.apache.commons.io.FilenameUtils;
import org.avasquez.seccloudfs.filesystem.File;
import org.avasquez.seccloudfs.filesystem.FileContent;
import org.avasquez.seccloudfs.filesystem.FileSystem;
import org.avasquez.seccloudfs.filesystem.db.dao.FileOperationDao;
import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;
import org.avasquez.seccloudfs.secure.storage.SecureCloudStorage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents a file whose content is stored in the cloud.
 *
 * @author avasquez
 */
public class CloudFile implements MetadataAwareFile {

    private FileMetadata metadata;
    private Path cachedFileContentRoot;
    private FileSystem fileSystem;
    private SecureCloudStorage cloudStorage;
    private FileOperationDao fileOperationDao;

    private CloudStorageUpdater cloudStorageUpdater;
    private Lock lock;

    public CloudFile(FileMetadata metadata, Path cachedFileContentRoot, FileSystem fileSystem,
                     SecureCloudStorage cloudStorage, FileOperationDao fileOperationDao, long nextUpdateTimeout,
                     Executor fileUploaderExecutor) {
        this.metadata = metadata;
        this.cachedFileContentRoot = cachedFileContentRoot;
        this.fileSystem = fileSystem;
        this.cloudStorage = cloudStorage;
        this.fileOperationDao = fileOperationDao;
        this.cloudStorageUpdater = new CloudStorageUpdater(this, nextUpdateTimeout, cloudStorage, fileUploaderExecutor);
        this.lock = new ReentrantLock();
    }

    @Override
    public FileMetadata getMetadata() {
        return metadata;
    }

    @Override
    public String getName() {
        return FilenameUtils.getName(metadata.getPath());
    }

    @Override
    public String getPath() {
        return metadata.getPath();
    }

    @Override
    public String getParent() {
        return metadata.getParent();
    }

    @Override
    public File getParentDir() throws IOException {
        return fileSystem.getFile(getParent());
    }

    @Override
    public boolean isDirectory() {
        return metadata.isDirectory();
    }

    @Override
    public FileContent getContent() throws IOException {
        if (metadata.isDirectory()) {
            throw new IOException(String.format("Can't return content for %s since it's a directory", this));
        }

        FileContent content = new CloudFileContent(metadata, getContentPath(), cloudStorage, cloudStorageUpdater);
        content = new SynchronizedFileContent(content, lock);
        content = new OperationLoggingFileContent(content, metadata.getPath(), fileOperationDao);

        return content;
    }

    @Override
    public Date getLastAccess() {
        return metadata.getLastAccess();
    }

    @Override
    public Date getLastModified() {
        return metadata.getLastModified();
    }

    @Override
    public void setLastAccess(Date date) {
        metadata.setLastAccess(date);
    }

    @Override
    public void setLastModified(Date date) {
        metadata.setLastModified(date);
    }

    @Override
    public String toString() {
        return metadata.getPath();
    }

    private Path getContentPath() {
        return cachedFileContentRoot.resolve(getPath());
    }

}
