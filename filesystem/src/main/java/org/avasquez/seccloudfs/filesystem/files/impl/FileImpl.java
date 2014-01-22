package org.avasquez.seccloudfs.filesystem.files.impl;

import org.avasquez.seccloudfs.filesystem.content.Content;
import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;
import org.avasquez.seccloudfs.filesystem.exception.FileNotFoundException;
import org.avasquez.seccloudfs.filesystem.files.File;
import org.avasquez.seccloudfs.filesystem.files.FileStore;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by alfonsovasquez on 19/01/14.
 */
public class FileImpl implements MetadataAwareFile, ContentAwareFile {

    private FileStore fileStore;
    private FileMetadata metadata;
    private Content content;

    private volatile Map<String, String> childrenMap;

    public FileImpl(FileStore fileStore, FileMetadata metadata, Content content) {
        this.fileStore = fileStore;
        this.metadata = metadata;
        this.content = content;
    }

    @Override
    public String getId() {
        return metadata.getId();
    }

    @Override
    public String getName() {
        return metadata.getName();
    }

    @Override
    public String getParentId() {
        return metadata.getParentId();
    }

    @Override
    public boolean isDirectory() {
        return metadata.isDirectory();
    }

    @Override
    public File getChild(String name) throws IOException {
        getChildrenMap();

        if (childrenMap != null) {
            String id = childrenMap.get(name);
            if (id != null) {
                return fileStore.find(id);
            }
        }

        return null;
    }

    @Override
    public List<File> getChildren() throws IOException {
        getChildrenMap();

        if (childrenMap != null) {
            Collection<String> childIds = childrenMap.values();
            List<File> children = new ArrayList<>();

            for (String id : childIds) {
                File child = fileStore.find(id);

                if (child == null) {
                    throw new FileNotFoundException("File '" + id + "' not found");
                }

                children.add(child);
            }

            return children;
        }

        return null;
    }

    @Override
    public Map<String, String> getChildrenMap() throws IOException {
        if (isDirectory() && childrenMap == null) {
            synchronized (this) {
                if (childrenMap == null) {
                    childrenMap = new ConcurrentHashMap<>();
                    List<File> children = fileStore.findChildren(getId());

                    if (children != null) {
                        for (File child : children) {
                            childrenMap.put(child.getName(), child.getId());
                        }
                    }
                }
            }
        }

        return childrenMap;
    }

    @Override
    public SeekableByteChannel getByteChannel() throws IOException {
        if (!isDirectory()) {
            return content.getByteChannel();
        } else {
            return null;
        }
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
    public FileMetadata getMetadata() {
        return metadata;
    }

    @Override
    public Content getContent() {
        return content;
    }

}
