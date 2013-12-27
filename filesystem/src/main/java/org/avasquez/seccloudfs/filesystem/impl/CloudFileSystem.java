package org.avasquez.seccloudfs.filesystem.impl;

import org.apache.commons.io.FilenameUtils;
import org.avasquez.seccloudfs.filesystem.File;
import org.avasquez.seccloudfs.filesystem.db.dao.FileMetadataDao;
import org.avasquez.seccloudfs.filesystem.db.dao.FileOperationDao;
import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;
import org.avasquez.seccloudfs.filesystem.exception.DirectoryNotEmptyException;
import org.avasquez.seccloudfs.filesystem.exception.FileExistsException;
import org.avasquez.seccloudfs.filesystem.exception.FileSystemException;
import org.avasquez.seccloudfs.filesystem.exception.NotSuchFileException;
import org.avasquez.seccloudfs.secure.storage.SecureCloudStorage;

import java.io.IOException;
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

    public static final char FILE_SEPARATOR = '/';

    private FileMetadataDao fileMetadataDao;
    private FileOperationDao fileOperationDao;
    private long fileChunkSize;
    private Path cachedFileContentRoot;
    private SecureCloudStorage cloudStorage;
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

    public void setFileOperationDao(FileOperationDao fileOperationDao) {
        this.fileOperationDao = fileOperationDao;
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
            return new CloudFile(metadata,
                    cachedFileContentRoot,
                    this,
                    cloudStorage,
                    fileOperationDao,
                    nextUpdateTimeout,
                    fileUploaderExecutor);
        } else {
            return null;
        }
    }

    @Override
    protected File doCreateFile(String path, boolean dir) throws FileSystemException {
        if (exists(path)) {
            throw new FileExistsException(String.format("File %s already exists", path));
        }

        String parentPath = FilenameUtils.getFullPathNoEndSeparator(path);
        if (exists(parentPath)) {
            FileMetadata metadata = new FileMetadata();
            metadata.setPath(path);
            metadata.setParent(parentPath);
            metadata.setDirectory(dir);
            metadata.setLastModified(new Date());
            if (!dir) {
                metadata.setContentId(UUID.randomUUID().toString());
                metadata.setChunkSize(fileChunkSize);
                metadata.setCachedChunks(new BitSet());
                metadata.setSize(0);
            }

            fileMetadataDao.insert(metadata);

            return new CloudFile(metadata,
                    cachedFileContentRoot,
                    this,
                    cloudStorage,
                    fileOperationDao,
                    nextUpdateTimeout,
                    fileUploaderExecutor);
        } else {
            throw new NotSuchFileException(String.format("Directory %s not found", parentPath));
        }
    }

    @Override
    protected List<File> doGetChildren(String path) throws FileSystemException {
        MetadataAwareFile file = (MetadataAwareFile) getFile(path);
        if (file == null) {
            throw new NotSuchFileException(String.format("File %s not found", path));
        }
        if (!file.isDirectory()) {
            throw new FileSystemException(String.format("File %s is not a directory", path));
        }

        List<File> children = new ArrayList<File>();
        List<FileMetadata> childrenMetadata = fileMetadataDao.findChildren(path);

        if (childrenMetadata != null) {
            for (FileMetadata metadata : childrenMetadata) {
                children.add(new CloudFile(metadata,
                        cachedFileContentRoot,
                        this,
                        cloudStorage,
                        fileOperationDao,
                        nextUpdateTimeout,
                        fileUploaderExecutor));
            }
        }

        return children;
    }

    @Override
    protected void doDeleteFile(String path) throws FileSystemException {
        MetadataAwareFile file = (MetadataAwareFile) getFile(path);
        if (file == null) {
            throw new NotSuchFileException(String.format("File %s not found", path));
        }

        if (file.isDirectory()) {
            List<File> children = getChildren(path);
            if (children != null && !children.isEmpty())  {
                throw new DirectoryNotEmptyException(String.format("Directory %s is not empty", path));
            }
        } else {
            try {
                file.getContent().delete();
            } catch (IOException e) {
                throw new FileSystemException(String.format("Error while deleting content of %s", path), e);
            }
        }

        fileMetadataDao.delete(path);
    }

    @Override
    protected File doCopyFile(String srcPath, String dstPath) throws FileSystemException {
        MetadataAwareFile srcFile = (MetadataAwareFile) getFile(srcPath);
        if (srcFile == null) {
            throw new NotSuchFileException(String.format("File %s not found", srcPath));
        }

        MetadataAwareFile dstFile = (MetadataAwareFile) getFile(dstPath);
        if (dstFile == null) {
            dstFile = (MetadataAwareFile) createFile(dstPath, srcFile.isDirectory());
        } else {
            throw new FileExistsException(String.format("File %s already exists", dstFile));
        }

        FileMetadata srcMetadata = srcFile.getMetadata();
        FileMetadata dstMetadata = dstFile.getMetadata();

        dstMetadata.setLastAccess(srcMetadata.getLastAccess());
        dstMetadata.setLastModified(srcMetadata.getLastModified());

        if (srcFile.isDirectory()) {
            if (!dstFile.isDirectory()) {
                throw new FileSystemException(String.format("Can't copy %s to %s because the source is a " +
                        "directory and the destination is not a directory", srcFile, dstFile));
            }

            // Copy children recursively
            List<File> children = getChildren(srcFile.getPath());
            if (children != null) {
                for (File child : children) {
                    copyFile(child.getPath(), dstPath + FILE_SEPARATOR + child.getName());
                }
            }
        } else {
            if (dstFile.isDirectory()) {
                throw new FileSystemException(String.format("Can't copy %s to %s because the source is not " +
                        "a directory and the destination is a directory", srcFile, dstFile));
            }

            try {
                srcFile.getContent().copyTo(dstFile.getContent());
            } catch (IOException e) {
                throw new FileSystemException(String.format("Error while copying content from %s to %s",
                        srcFile, dstFile), e);
            }
        }

        fileMetadataDao.update(dstMetadata);

        return dstFile;
    }

    @Override
    protected File doMoveFile(String srcPath, String dstPath) throws FileSystemException {
        MetadataAwareFile srcFile = (MetadataAwareFile) getFile(srcPath);
        if (srcFile == null) {
            throw new NotSuchFileException(String.format("File %s not found", srcPath));
        }

        MetadataAwareFile dstFile = (MetadataAwareFile) getFile(dstPath);
        if (dstFile == null) {
            dstFile = (MetadataAwareFile) createFile(dstPath, srcFile.isDirectory());
        } else {
            throw new FileExistsException(String.format("File %s already exists", dstFile));
        }

        FileMetadata srcMetadata = srcFile.getMetadata();
        FileMetadata dstMetadata = dstFile.getMetadata();

        dstMetadata.setContentId(srcMetadata.getContentId());
        dstMetadata.setChunkSize(srcMetadata.getChunkSize());
        dstMetadata.setCachedChunks(srcMetadata.getCachedChunks());
        dstMetadata.setSize(srcMetadata.getSize());
        dstMetadata.setLastAccess(srcMetadata.getLastAccess());
        dstMetadata.setLastModified(srcMetadata.getLastModified());

        fileMetadataDao.update(dstMetadata);

        deleteFile(srcPath);

        return dstFile;
    }

}
