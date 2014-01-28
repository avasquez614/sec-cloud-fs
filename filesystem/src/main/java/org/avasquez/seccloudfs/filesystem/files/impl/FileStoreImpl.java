package org.avasquez.seccloudfs.filesystem.files.impl;

import org.avasquez.seccloudfs.filesystem.content.Content;
import org.avasquez.seccloudfs.filesystem.content.ContentStore;
import org.avasquez.seccloudfs.filesystem.db.dao.FileMetadataDao;
import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;
import org.avasquez.seccloudfs.filesystem.exception.DirectoryNotEmptyException;
import org.avasquez.seccloudfs.filesystem.exception.FileExistsException;
import org.avasquez.seccloudfs.filesystem.exception.FileNotDirectoryException;
import org.avasquez.seccloudfs.filesystem.exception.FileNotFoundException;
import org.avasquez.seccloudfs.filesystem.files.File;
import org.avasquez.seccloudfs.filesystem.files.User;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by alfonsovasquez on 19/01/14.
 */
public class FileStoreImpl extends AbstractCachedFileStore {

    private static final Logger logger = Logger.getLogger(FileStoreImpl.class.getName());

    private FileMetadataDao metadataDao;
    private ContentStore contentStore;

    public void setMetadataDao(FileMetadataDao metadataDao) {
        this.metadataDao = metadataDao;
    }

    public void setContentStore(ContentStore contentStore) {
        this.contentStore = contentStore;
    }

    @Override
    protected File doGetRoot() throws IOException {
        FileMetadata metadata = metadataDao.getRoot();
        if (metadata != null) {
            return new FileImpl(this, metadata, metadataDao, null);
        } else {
            return null;
        }
    }

    @Override
    protected File doCreateRoot(User owner, long permissions) throws IOException {
        Date now = new Date();

        FileMetadata metadata = new FileMetadata();
        metadata.setParentId(null);
        metadata.setDirectory(true);
        metadata.setLastChangeTime(now);
        metadata.setLastModifiedTime(now);
        metadata.setLastAccessTime(now);
        metadata.setOwner(owner);
        metadata.setPermissions(permissions);

        metadataDao.insert(metadata);

        return new FileImpl(this, metadata, metadataDao, null);
    }

    @Override
    protected File doFind(String id) throws IOException {
        FileMetadata metadata = metadataDao.find(id);
        if (metadata != null) {
            return new FileImpl(this, metadata, metadataDao, getContent(metadata));
        } else {
            return null;
        }
    }

    @Override
    protected File doCreate(File parent, String name, boolean dir, User owner, long permissions)
            throws IOException {
        if (!parent.isDirectory()) {
            throw new FileNotDirectoryException("File '" + parent.getId() + "' is not a directory");
        }
        if (parent.hasChild(name)) {
            throw new FileExistsException("Directory '" + parent.getId() + "' already contains a file " +
                    "with name '" + name + "'");
        }

        Date now = new Date();

        FileMetadata metadata = new FileMetadata();
        metadata.setParentId(parent.getId());
        metadata.setDirectory(dir);
        metadata.setLastChangeTime(now);
        metadata.setLastModifiedTime(now);
        metadata.setLastAccessTime(now);
        metadata.setOwner(owner);
        metadata.setPermissions(permissions);

        Content content = null;
        if (!dir) {
            content = contentStore.create();
            metadata.setContentId(content.getId());
        }

        metadataDao.insert(metadata);

        File file = new FileImpl(this, metadata, metadataDao, content);

        parent.addChild(name, file.getId());

        return file;
    }

    @Override
    protected File doMove(File file, File newParent, String newName) throws IOException {
        synchronized (file) {
            File oldParent = find(file.getParentId());
            if (oldParent == null) {
                throw new FileNotFoundException("File '" + oldParent.getId() + "' not found");
            }

            String oldName = oldParent.getChildName(file.getId());
            if (oldName == null) {
                throw new IOException("The file '" + file.getId() + "' was removed from directory '" +
                        oldParent.getId() + "' before the move could be done");
            }

            if (!oldParent.equals(newParent)) {
                if (!newParent.isDirectory()) {
                    throw new FileNotDirectoryException("File '" + newParent.getId() + "' is not a directory");
                }
            } else if (!oldName.equals(newName)) {
                newParent = oldParent;
            } else {
                return file;
            }

            File replacedFile = null;

            if (newParent.hasChild(newName)) {
                replacedFile = newParent.getChild(newName);
            }

            FileMetadata metadata = ((MetadataAwareFile) file).getMetadata();
            metadata.setParentId(newParent.getId());

            metadataDao.update(metadata);

            newParent.addChild(newName, metadata.getId());
            oldParent.removeChild(oldName);

            if (replacedFile != null) {
                delete(replacedFile);
            }
        }

        return file;
    }

    @Override
    protected void doDelete(File file) throws IOException {
        if (file.isDirectory() && !file.isEmpty()) {
            throw new DirectoryNotEmptyException("Directory '" + file.getId() + "' should be empty before deleting");
        }

        File parent = find(file.getParentId());
        if (parent == null) {
            throw new FileNotFoundException("File '" + parent.getId() + "' not found");
        }

        synchronized (file) {
            metadataDao.delete(file.getId());

            if (!file.isDirectory()) {
                contentStore.delete(((ContentAwareFile) file).getContent().getId());
            }

            parent.removeChildById(file.getId());
        }
    }

    private Content getContent(FileMetadata metadata) throws IOException {
        if (!metadata.isDirectory()) {
            Content content = contentStore.find(metadata.getContentId());
            if (content == null) {
                logger.log(Level.INFO, "Content '{0}' not found for file '{1}'. Creating new one...",
                        new Object[]{metadata.getContentId(), metadata.getId()});

                content = contentStore.create();

                metadata.setContentId(content.getId());
                metadataDao.update(metadata);
            }

            return content;
        } else {
            return null;
        }
    }

}
