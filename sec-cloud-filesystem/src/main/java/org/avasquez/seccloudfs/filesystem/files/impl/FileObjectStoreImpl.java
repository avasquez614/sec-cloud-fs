package org.avasquez.seccloudfs.filesystem.files.impl;

import java.io.IOException;
import java.util.Date;

import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.filesystem.content.Content;
import org.avasquez.seccloudfs.filesystem.content.ContentStore;
import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;
import org.avasquez.seccloudfs.filesystem.db.repos.DirectoryEntryRepository;
import org.avasquez.seccloudfs.filesystem.db.repos.FileMetadataRepository;
import org.avasquez.seccloudfs.filesystem.files.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Created by alfonsovasquez on 19/01/14.
 */
public class FileObjectStoreImpl extends AbstractCachedFileObjectStore {

    private static final Logger logger = LoggerFactory.getLogger(FileObjectStoreImpl.class);

    private FileMetadataRepository metadataRepo;
    private DirectoryEntryRepository entryRepo;
    private ContentStore contentStore;

    @Required
    public void setMetadataRepo(FileMetadataRepository metadataRepo) {
        this.metadataRepo = metadataRepo;
    }

    @Required
    public void setEntryRepo(DirectoryEntryRepository entryRepo) {
        this.entryRepo = entryRepo;
    }

    @Required
    public void setContentStore(ContentStore contentStore) {
        this.contentStore = contentStore;
    }

    @Override
    public long getTotalFiles() throws IOException {
        try {
            return metadataRepo.count();
        } catch (DbException e) {
            throw new IOException("Unable to return count of file metadata in DB", e);
        }
    }

    @Override
    protected FileObject doFind(String id) throws IOException {
        FileMetadata metadata;
        try {
            metadata = metadataRepo.find(id);
        } catch (DbException e) {
            throw new IOException("Unable to find file metadata for ID '" + id + "'", e);
        }

        if (metadata != null) {
            return new FileObjectImpl(this, metadata, metadataRepo, getDirectoryEntries(metadata),
                    getContent(metadata));
        } else {
            return null;
        }
    }

    @Override
    protected FileObject doCreate(boolean dir, User owner, long permissions) throws IOException {
        Date now = new Date();

        FileMetadata metadata = new FileMetadata();
        metadata.setDirectory(dir);
        metadata.setLastChangeTime(now);
        metadata.setLastModifiedTime(now);
        metadata.setLastAccessTime(now);
        metadata.setOwner(owner);
        metadata.setPermissions(permissions);

        DirectoryEntries entries = null;
        Content content = null;

        if (!dir) {
            content = contentStore.create();
            metadata.setContentId(content.getId());
        }

        try {
            metadataRepo.insert(metadata);
        } catch (DbException e) {
            throw new IOException("Unable to insert " + metadata + " into DB", e);
        }

        if (dir) {
            entries = new DirectoryEntries(entryRepo, metadata.getId());
        }

        FileObject file = new FileObjectImpl(this, metadata, metadataRepo, entries, content);

        logger.debug("{} created", file);

        return file;
    }

    @Override
    protected void doDelete(FileObject file) throws IOException {
        try {
            metadataRepo.delete(file.getId());
        } catch (DbException e) {
            throw new IOException("Unable to delete " + file + " from DB", e);
        }

        logger.debug("{} deleted", file);

        if (!file.isDirectory()) {
            contentStore.delete(file.getContent());
        }
    }

    private DirectoryEntries getDirectoryEntries(FileMetadata metadata) throws IOException {
        if (metadata.isDirectory()) {
            return new DirectoryEntries(entryRepo, metadata.getId());
        } else {
            return null;
        }
    }

    private Content getContent(FileMetadata metadata) throws IOException {
        if (!metadata.isDirectory()) {
            Content content = contentStore.find(metadata.getContentId());
            if (content == null) {
                logger.warn("Content '{}' not found for file '{}'. Creating new one...",
                        metadata.getContentId(),
                        metadata.getId());

                content = contentStore.create();

                metadata.setContentId(content.getId());
                try {
                    metadataRepo.save(metadata);
                } catch (DbException e) {
                    throw new IOException("Unable to save " + metadata + " in DB", e);
                }
            }

            return content;
        } else {
            return null;
        }
    }

}
