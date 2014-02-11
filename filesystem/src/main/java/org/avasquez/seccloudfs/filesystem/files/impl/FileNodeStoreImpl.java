package org.avasquez.seccloudfs.filesystem.files.impl;

import org.avasquez.seccloudfs.filesystem.content.Content;
import org.avasquez.seccloudfs.filesystem.content.ContentStore;
import org.avasquez.seccloudfs.filesystem.db.dao.DirectoryEntryDao;
import org.avasquez.seccloudfs.filesystem.db.dao.FileMetadataDao;
import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;
import org.avasquez.seccloudfs.filesystem.files.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import java.io.IOException;
import java.util.Date;

/**
 * Created by alfonsovasquez on 19/01/14.
 */
public class FileNodeStoreImpl extends AbstractCachedFileNodeStore {

    private static final Logger logger = LoggerFactory.getLogger(FileNodeStoreImpl.class);

    private FileMetadataDao metadataDao;
    private DirectoryEntryDao entryDao;
    private ContentStore contentStore;

    @Required
    public void setMetadataDao(FileMetadataDao metadataDao) {
        this.metadataDao = metadataDao;
    }

    @Required
    public void setEntryDao(DirectoryEntryDao entryDao) {
        this.entryDao = entryDao;
    }

    @Required
    public void setContentStore(ContentStore contentStore) {
        this.contentStore = contentStore;
    }

    @Override
    protected FileNode doFind(String id) throws IOException {
        FileMetadata metadata = metadataDao.find(id);
        if (metadata != null) {
            return new FileNodeImpl(this, metadata, metadataDao, getDirectoryEntries(metadata), getContent(metadata));
        } else {
            return null;
        }
    }

    @Override
    protected FileNode doCreate(boolean dir, User owner, long permissions) throws IOException {
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

        if (dir) {
            entries = new DirectoryEntries(entryDao, metadata.getId());
        } else {
            content = contentStore.create();
            metadata.setContentId(content.getId());
        }

        metadataDao.insert(metadata);

        return new FileNodeImpl(this, metadata, metadataDao, entries, content);
    }

    @Override
    protected void doDelete(FileNode file) throws IOException {
        metadataDao.delete(file.getId());

        if (!file.isDirectory()) {
            contentStore.delete(file.getContent());
        }
    }

    private DirectoryEntries getDirectoryEntries(FileMetadata metadata) throws IOException {
        if (metadata.isDirectory()) {
            return new DirectoryEntries(entryDao, metadata.getId());
        } else {
            return null;
        }
    }

    private Content getContent(FileMetadata metadata) throws IOException {
        if (!metadata.isDirectory()) {
            Content content = contentStore.find(metadata.getContentId());
            if (content == null) {
                logger.info("Content '{}' not found for file '{}'. Creating new one...",
                        metadata.getContentId(),
                        metadata.getId());

                content = contentStore.create();

                metadata.setContentId(content.getId());
                metadataDao.save(metadata);
            }

            return content;
        } else {
            return null;
        }
    }

}
