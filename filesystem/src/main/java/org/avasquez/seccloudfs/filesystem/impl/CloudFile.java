package org.avasquez.seccloudfs.filesystem.impl;

import org.apache.commons.io.FilenameUtils;
import org.avasquez.seccloudfs.filesystem.File;
import org.avasquez.seccloudfs.filesystem.FileContent;
import org.avasquez.seccloudfs.filesystem.FileSystem;
import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;
import org.avasquez.seccloudfs.secure.storage.SecureCloudStorage;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
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
    private WriteLog writeLog;

    private FileUploader fileUploader;
    private Lock lock;

    public CloudFile(FileMetadata metadata, Path cachedFileContentRoot, FileSystem fileSystem,
                     SecureCloudStorage cloudStorage, WriteLog writeLog, long nextUpdateTimeout,
                     Executor fileUploaderExecutor) {
        this.metadata = metadata;
        this.cachedFileContentRoot = cachedFileContentRoot;
        this.fileSystem = fileSystem;
        this.cloudStorage = cloudStorage;
        this.writeLog = writeLog;
        this.fileUploader = new FileUploader(this, nextUpdateTimeout, cloudStorage, fileUploaderExecutor);
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

        Path path = cachedFileContentRoot.resolve(getPath());
        FileChannel content;
        FileContent fileContent;

        try {
            content = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new IOException(String.format("Unable to open content at %s", this), e);
        }

        fileContent = new CloudFileContent(metadata, content, cloudStorage, fileUploader);
        fileContent = new SynchronizedFileContent(fileContent, lock);
        fileContent = new LoggingFileContent(fileContent, metadata.getPath(), writeLog);

        return fileContent;
    }

    @Override
    public void copyContentTo(File file) throws IOException {
        FileMetadata targetMetadata = ((MetadataAwareFile) file).getMetadata();

        if (metadata.isDirectory()) {
            throw new IOException(String.format("Can't copy content from %s since it's a directory", this));
        }
        if (targetMetadata.isDirectory()) {
            throw new IOException(String.format("Can't copy content to %s since it's a directory", this));
        }

        FileContent content = getContent();
        content.downloadAll();
        content.close();

        Path srcPath = cachedFileContentRoot.resolve(getPath());
        Path targetPath = cachedFileContentRoot.resolve(file.getPath());

        lock.lock();
        try {
            Files.copy(srcPath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            targetMetadata.setCachedChunks(metadata.getCachedChunks());
            targetMetadata.setSize(metadata.getSize());
        } finally {
            lock.unlock();
        }
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

}
