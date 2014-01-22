package org.avasquez.seccloudfs.filesystem.files.impl;

import org.avasquez.seccloudfs.filesystem.content.Content;
import org.avasquez.seccloudfs.filesystem.content.ContentStore;
import org.avasquez.seccloudfs.filesystem.db.dao.FileMetadataDao;
import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;
import org.avasquez.seccloudfs.filesystem.exception.DirectoryNotEmptyException;
import org.avasquez.seccloudfs.filesystem.exception.FileExistsException;
import org.avasquez.seccloudfs.filesystem.exception.FileNotFoundException;
import org.avasquez.seccloudfs.filesystem.exception.UnexpectedFileTypeException;
import org.avasquez.seccloudfs.filesystem.files.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by alfonsovasquez on 19/01/14.
 */
public class FileStoreImpl extends AbstractCachedFileStore {

    private static final Logger logger = LoggerFactory.getLogger(FileStoreImpl.class);

    private FileMetadataDao metadataDao;
    private ContentStore contentStore;

    public void setMetadataDao(FileMetadataDao metadataDao) {
        this.metadataDao = metadataDao;
    }

    public void setContentStore(ContentStore contentStore) {
        this.contentStore = contentStore;
    }

    @Override
    protected File doFind(String id) throws IOException {
        FileMetadata metadata = metadataDao.find(id);
        if (metadata != null) {
            return new FileImpl(this, metadata, getContent(metadata));
        } else {
            return null;
        }
    }

    @Override
    protected List<File> doFindChildren(String id) throws IOException {
        File file = find(id);

        if (file == null) {
            throw new FileNotFoundException("File '" + id + "' not found");
        }
        if (!file.isDirectory()) {
            throw new UnexpectedFileTypeException("File '" + id + "' is not a directory");
        }

        List<File> children = new ArrayList<>();
        List<FileMetadata> childrenMetadata = metadataDao.findChildren(id);

        if (childrenMetadata != null) {
            for (FileMetadata metadata : childrenMetadata) {
                children.add(new FileImpl(this, metadata, getContent(metadata)));
            }
        }

        return children;
    }

    @Override
    protected File doCreate(String parentId, String name, boolean dir) throws IOException {
        File parent = find(parentId);

        if (parent == null) {
            throw new FileNotFoundException("File '" + parentId + "' not found");
        }
        if (!parent.isDirectory()) {
            throw new UnexpectedFileTypeException("File '" + parentId + "' is not a directory");
        }
        if (parent.getChildrenMap().containsKey(name)) {
            throw new FileExistsException("Directory '" + parentId + "' already contains a file with name '" +
                    name + "'");
        }

        FileMetadata metadata = new FileMetadata();
        metadata.setParentId(parentId);
        metadata.setName(name);
        metadata.setDirectory(dir);
        metadata.setLastModifiedTime(new Date());
        metadata.setLastAccessTime(new Date());

        Content content = null;
        if (!dir) {
            content = contentStore.create();
            metadata.setContentId(content.getId());
        }

        metadataDao.insert(metadata);

        File file = new FileImpl(this, metadata, content);

        parent.getChildrenMap().put(name, file.getId());

        return file;
    }

    @Override
    protected File doRename(String id, String newName) throws IOException {
        File file = find(id);

        if (file != null) {
            throw new FileNotFoundException("File '" + id + "' not found");
        }

        synchronized (file) {
            File parent = find(file.getParentId());

            if (parent == null) {
                throw new FileNotFoundException("File '" + parent.getId() + "' not found");
            }
            if (parent.getChildrenMap().containsKey(newName)) {
                throw new FileExistsException("Directory '" + parent.getId() + "' already contains a file " +
                        "with name '" + newName + "'");
            }

            FileMetadata metadata = ((MetadataAwareFile) file).getMetadata();
            String oldName = metadata.getName();

            metadata.setName(newName);

            metadataDao.update(metadata);

            parent.getChildrenMap().put(newName, metadata.getId());
            parent.getChildrenMap().remove(oldName);
        }

        return file;
    }

    @Override
    protected File doMove(String id, String newParentId, String newName) throws IOException {
        File file = find(id);

        if (file != null) {
            throw new FileNotFoundException("File '" + id + "' not found");
        }

        synchronized (file) {
            File oldParent = find(file.getParentId());

            if (oldParent == null) {
                throw new FileNotFoundException("File '" + oldParent.getId() + "' not found");
            }

            File newParent = find(newParentId);

            if (newParent == null) {
                throw new FileNotFoundException("File '" + newParentId + "' not found");
            }
            if (!newParent.isDirectory()) {
                throw new UnexpectedFileTypeException("File '" + newParentId + "' is not a directory");
            }
            if (newParent.getChildrenMap().containsKey(newName)) {
                throw new FileExistsException("Directory '" + newParentId + "' already contains a file with " +
                        "name '" + newName + "'");
            }

            FileMetadata metadata = ((MetadataAwareFile) file).getMetadata();
            String oldName = metadata.getName();

            metadata.setParentId(newParentId);
            metadata.setName(newName);

            metadataDao.update(metadata);

            newParent.getChildrenMap().put(newName, metadata.getId());
            oldParent.getChildrenMap().remove(oldName);
        }

        return file;
    }

    @Override
    protected void doDelete(String id) throws IOException {
        ContentAwareFile file = (ContentAwareFile) find(id);

        if (file == null) {
            throw new FileNotFoundException("File '" + id + "' not found");
        }
        if (file.isDirectory() && !file.getChildrenMap().isEmpty()) {
            throw new DirectoryNotEmptyException("Directory '" + id + "' should be empty before deleting");
        }

        File parent = find(file.getParentId());

        if (parent == null) {
            throw new FileNotFoundException("File '" + parent.getId() + "' not found");
        }

        synchronized (file) {
            metadataDao.delete(id);

            if (!file.isDirectory()) {
                contentStore.delete(file.getContent().getId());
            }

            parent.getChildren().remove(file.getName());
        }
    }

    private Content getContent(FileMetadata metadata) throws IOException {
        if (!metadata.isDirectory()) {
            Content content = contentStore.find(metadata.getContentId());
            if (content == null) {
                logger.warn("Content '{}' not found for file '{}'. Creating new one...", metadata.getContentId(),
                        metadata.getId());

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
