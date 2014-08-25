package org.avasquez.seccloudfs.filesystem.files.impl;

import java.io.IOException;
import java.util.Date;

import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.filesystem.content.Content;
import org.avasquez.seccloudfs.filesystem.db.model.DirectoryEntry;
import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;
import org.avasquez.seccloudfs.filesystem.db.repos.FileMetadataRepository;
import org.avasquez.seccloudfs.filesystem.exception.DirectoryNotEmptyException;
import org.avasquez.seccloudfs.filesystem.exception.NotDirectoryException;
import org.avasquez.seccloudfs.filesystem.files.File;
import org.avasquez.seccloudfs.filesystem.files.User;
import org.avasquez.seccloudfs.filesystem.util.FlushableByteChannel;

/**
 * Created by alfonsovasquez on 19/01/14.
 */
public class FileObjectImpl implements FileObject {

    private FileObjectStore fileObjectStore;
    private FileMetadata metadata;
    private FileMetadataRepository metadataRepo;
    private DirectoryEntries entries;
    private Content content;

    public FileObjectImpl(FileObjectStore fileObjectStore, FileMetadata metadata, FileMetadataRepository metadataRepo,
                          DirectoryEntries entries, Content content) {
        this.fileObjectStore = fileObjectStore;
        this.metadata = metadata;
        this.metadataRepo = metadataRepo;
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
                return fileObjectStore.find(entry.getFileId());
            }
        }

        return null;
    }

    @Override
    public boolean hasChild(String name) throws IOException {
        if (entries != null) {
            return entries.hasEntry(name);
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
        FileObject file = fileObjectStore.create(dir, owner, permissions);

        entries.createEntry(name, file.getId());

        return file;
    }

    @Override
    public synchronized File moveFileTo(String name, File newParent, String newName) throws IOException {
        FileObject file = (FileObject) getChild(name);
        if (file != null) {
            FileObject newParentFile = (FileObject) newParent;

            if (!equals(newParentFile)) {
                if (!newParentFile.isDirectory()) {
                    throw new NotDirectoryException("File " + newParentFile + " is not a directory");
                }
            } else if (!name.equals(newName)) {
                newParentFile = this;
            } else {
                return file;
            }

            FileObject replacedFile = null;

            if (newParentFile.hasChild(newName)) {
                replacedFile = (FileObject) newParentFile.getChild(newName);
            }

            entries.moveEntryTo(name, newParentFile.getEntries(), newName);

            if (replacedFile != null) {
                fileObjectStore.delete(replacedFile);
            }
        }

        return file;
    }

    @Override
    public synchronized void delete(String name) throws IOException {
        FileObject file = (FileObject) getChild(name);
        if (file != null) {
            if (file.isDirectory() && !file.isEmpty()) {
                throw new DirectoryNotEmptyException("Directory '" + file + "' should be empty before deleting");
            }

            entries.deleteEntry(name);

            fileObjectStore.delete(file);
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
        try {
            metadataRepo.save(metadata);
        } catch (DbException e) {
            throw new IOException("Unable to save " + metadata + " to DB", e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FileObjectImpl file = (FileObjectImpl) o;

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
        return "FileObjectImpl{" +
                "metadata=" + metadata +
                ", entries=" + entries +
                '}';
    }

}
