package org.avasquez.seccloudfs.filesystem.files.impl;

import org.avasquez.seccloudfs.filesystem.content.Content;
import org.avasquez.seccloudfs.filesystem.db.model.DirectoryEntry;
import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;
import org.avasquez.seccloudfs.filesystem.db.repos.FileMetadataRepository;
import org.avasquez.seccloudfs.filesystem.exception.DirectoryNotEmptyException;
import org.avasquez.seccloudfs.filesystem.exception.FileNotDirectoryException;
import org.avasquez.seccloudfs.filesystem.files.File;
import org.avasquez.seccloudfs.filesystem.files.User;
import org.avasquez.seccloudfs.filesystem.util.FlushableByteChannel;

import java.io.IOException;
import java.util.Date;

/**
 * Created by alfonsovasquez on 19/01/14.
 */
public class FileNodeImpl implements FileNode {

    private FileNodeStore fileNodeStore;
    private FileMetadata metadata;
    private FileMetadataRepository metadataRepository;
    private DirectoryEntries entries;
    private Content content;

    public FileNodeImpl(FileNodeStore fileNodeStore, FileMetadata metadata, FileMetadataRepository metadataRepository,
                        DirectoryEntries entries, Content content) {
        this.fileNodeStore = fileNodeStore;
        this.metadata = metadata;
        this.metadataRepository = metadataRepository;
        this.entries = entries;
        this.content = content;
    }

    @Override
    public String getId() {
        return metadata.getId();
    }

    @Override
    public long getSize() throws IOException {
        if (content != null) {
            return content.getSize();
        } else {
            return 0;
        }
    }

    @Override
    public boolean isDirectory() {
        return metadata.isDirectory();
    }

    @Override
    public boolean isEmpty() throws IOException {
        if (entries != null) {
            return entries.isEmpty();
        }

        return true;
    }

    @Override
    public File getChild(String name) throws IOException {
        if (entries != null) {
            DirectoryEntry entry = entries.getEntry(name);
            if (entry != null) {
                return fileNodeStore.find(entry.getFileId());
            }
        }

        return null;
    }

    @Override
    public boolean hasChild(String name) throws IOException {
        if (entries != null) {
            entries.hasEntry(name);
        }

        return false;
    }

    @Override
    public String[] getChildren() throws IOException {
        if (entries != null) {
            return entries.getFileNames();
        }

        return null;
    }

    @Override
    public DirectoryEntries getEntries() {
        return entries;
    }

    @Override
    public synchronized File createFile(String name, boolean dir, User owner, long permissions) throws IOException {
        FileNode file = fileNodeStore.create(dir, owner, permissions);

        entries.createEntry(name, file.getId());

        return file;
    }

    @Override
    public synchronized File moveFileTo(String name, File newParent, String newName) throws IOException {
        FileNode file = (FileNode) getChild(name);
        if (file != null) {
            FileNode newParentFile = (FileNode) newParent;

            if (!equals(newParentFile)) {
                if (!newParentFile.isDirectory()) {
                    throw new FileNotDirectoryException("File " + newParentFile + " is not a directory");
                }
            } else if (!name.equals(newName)) {
                newParentFile = this;
            } else {
                return file;
            }

            FileNode replacedFile = null;

            if (newParentFile.hasChild(newName)) {
                replacedFile = (FileNode) newParentFile.getChild(newName);
            }

            entries.moveEntryTo(name, newParentFile.getEntries(), newName);

            if (replacedFile != null) {
                fileNodeStore.delete(replacedFile);
            }
        }

        return file;
    }

    @Override
    public synchronized void delete(String name) throws IOException {
        FileNode file = (FileNode) getChild(name);
        if (file != null) {
            if (file.isDirectory() && !file.isEmpty()) {
                throw new DirectoryNotEmptyException("Directory '" + file + "' should be empty before deleting");
            }

            entries.deleteEntry(name);

            fileNodeStore.delete(file);
        }
    }

    @Override
    public Content getContent() {
        return content;
    }

    @Override
    public FlushableByteChannel getByteChannel() throws IOException {
        if (!isDirectory()) {
            return content.getByteChannel();
        } else {
            return null;
        }
    }

    @Override
    public Date getLastChangeTime() {
        return metadata.getLastChangeTime();
    }

    @Override
    public Date getLastAccessTime() {
        return metadata.getLastAccessTime();
    }

    @Override
    public Date getLastModifiedTime() {
        return metadata.getLastModifiedTime();
    }

    @Override
    public void setLastChangeTime(Date lastChangeTime) {
        metadata.setLastChangeTime(lastChangeTime);
    }

    @Override
    public void setLastAccessTime(Date lastAccessTime) {
        metadata.setLastAccessTime(lastAccessTime);
    }

    @Override
    public void setLastModifiedTime(Date lastModifiedTime) {
        metadata.setLastModifiedTime(lastModifiedTime);
    }

    @Override
    public User getOwner() {
        return metadata.getOwner();
    }

    @Override
    public void setOwner(User fileOwner) {
        metadata.setOwner(fileOwner);
    }

    @Override
    public long getPermissions() {
        return metadata.getPermissions();
    }

    @Override
    public void setPermissions(long permissions) {
        metadata.setPermissions(permissions);
    }

    @Override
    public void syncMetadata() throws IOException {
        metadataRepository.save(metadata);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FileNodeImpl file = (FileNodeImpl) o;

        if (!metadata.equals(file.metadata)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return metadata.hashCode();
    }

    @Override
    public String toString() {
        return "FileNodeImpl{" +
                "metadata=" + metadata +
                ", entries=" + entries +
                '}';
    }

}
