package org.avasquez.seccloudfs.filesystem.impl;

import org.apache.commons.io.FilenameUtils;
import org.avasquez.seccloudfs.filesystem.File;
import org.avasquez.seccloudfs.filesystem.db.dao.FileMetadataDao;
import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;
import org.avasquez.seccloudfs.filesystem.exception.FileSystemException;
import org.avasquez.seccloudfs.filesystem.exception.PathNotFoundException;
import org.avasquez.seccloudfs.secure.storage.SecureCloudStorage;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executor;

/**
 * Default implementation of {@link org.avasquez.seccloudfs.filesystem.FileSystem}, that works as an interface
 * to the secured files in the cloud.
 *
 * @author avasquez
 */
public class CloudFileSystem extends AbstractCachedFileSystem {

    private FileMetadataDao fileMetadataDao;
    private long fileChunkSize;
    private Path cachedFileContentRoot;
    private SecureCloudStorage cloudStorage;
    private WriteLog writeLog;
    private long nextUpdateTimeout;
    private Executor fileUploaderExecutor;

    public void setFileMetadataDao(FileMetadataDao fileMetadataDao) {
        this.fileMetadataDao = fileMetadataDao;
    }

    public void setFileChunkSize(long fileChunkSize) {
        this.fileChunkSize = fileChunkSize;
    }

    public void setCachedFileContentRoot(String cachedFileContentRoot) {
        this.cachedFileContentRoot = Paths.get(cachedFileContentRoot);
    }

    public void setCloudStorage(SecureCloudStorage cloudStorage) {
        this.cloudStorage = cloudStorage;
    }

    public void setWriteLog(WriteLog writeLog) {
        this.writeLog = writeLog;
    }

    public void setNextUpdateTimeout(long nextUpdateTimeout) {
        this.nextUpdateTimeout = nextUpdateTimeout;
    }

    public void setFileUploaderExecutor(Executor fileUploaderExecutor) {
        this.fileUploaderExecutor = fileUploaderExecutor;
    }

    @Override
    protected File doGetFile(String path) throws FileSystemException {
        FileMetadata metadata = fileMetadataDao.findByPath(path);
        if (metadata != null) {
            return new CloudFile(metadata, cachedFileContentRoot, this, cloudStorage, writeLog, nextUpdateTimeout,
                    fileUploaderExecutor);
        } else {
            return null;
        }
    }

    @Override
    protected File doCreateFile(String path, boolean dir) throws FileSystemException {
        String parentPath = FilenameUtils.getFullPathNoEndSeparator(path);
        if (exists(parentPath)) {
            FileMetadata metadata = new FileMetadata();
            metadata.setPath(path);
            metadata.setParent(parentPath);
            metadata.setDirectory(dir);
            metadata.setContentId(UUID.randomUUID().toString());
            metadata.setChunkSize(fileChunkSize);
            metadata.setCachedChunks(new BitSet());
            metadata.setSize(0);
            metadata.setLastModified(new Date());

            fileMetadataDao.save(metadata);

            return new CloudFile(metadata, cachedFileContentRoot, this, cloudStorage, writeLog, nextUpdateTimeout,
                    fileUploaderExecutor);
        } else {
            throw new PathNotFoundException("Path '" + parentPath + "' not found");
        }
    }

    @Override
    protected List<File> doGetChildren(String path) throws FileSystemException {
        List<File> children = new ArrayList<File>();
        List<FileMetadata> childrenMetadata = fileMetadataDao.findChildren(path);

        if (childrenMetadata != null) {
            for (FileMetadata metadata : childrenMetadata) {
                children.add(new CloudFile(metadata, cachedFileContentRoot, this, cloudStorage, writeLog,
                        nextUpdateTimeout, fileUploaderExecutor));
            }
        }

        return children;
    }

    @Override
    protected void doDeleteFile(String path) throws FileSystemException {

    }

    @Override
    protected File doCopyFile(String srcPath, String dstPath) throws FileSystemException {
        return null;
    }

    @Override
    protected File doMoveFile(String srcPath, String dstPath) throws FileSystemException {
        return null;
    }

}
