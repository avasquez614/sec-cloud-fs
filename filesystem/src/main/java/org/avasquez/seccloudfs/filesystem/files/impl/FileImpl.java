package org.avasquez.seccloudfs.filesystem.files.impl;

import org.avasquez.seccloudfs.filesystem.content.Content;
import org.avasquez.seccloudfs.filesystem.db.dao.FileMetadataDao;
import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;
import org.avasquez.seccloudfs.filesystem.exception.FileNotFoundException;
import org.avasquez.seccloudfs.filesystem.files.File;
import org.avasquez.seccloudfs.filesystem.files.FileStore;
import org.avasquez.seccloudfs.filesystem.files.User;
import org.avasquez.seccloudfs.filesystem.util.FlushableByteChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by alfonsovasquez on 19/01/14.
 */
public class FileImpl implements MetadataAwareFile, ContentAwareFile {

    private FileStore fileStore;
    private FileMetadata metadata;
    private FileMetadataDao metadataDao;
    private Content content;

    private volatile Map<String, String> childrenMap;

    public FileImpl(FileStore fileStore, FileMetadata metadata, FileMetadataDao metadataDao, Content content) {
        this.fileStore = fileStore;
        this.metadata = metadata;
        this.metadataDao = metadataDao;
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
    public File getParent() throws IOException {
        return fileStore.find(getParentId());
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
        if (childrenMap == null) {
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
    public FlushableByteChannel getByteChannel() throws IOException {
        if (!isDirectory()) {
            return new MetadataUpdaterByteChannel(content.getByteChannel());
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
    public User getOwner() {
        return metadata.getOwner();
    }

    @Override
    public void setOwner(User fileOwner) {
        metadata.setOwner(fileOwner);
        metadata.setLastChangeTime(new Date());
    }

    @Override
    public long getPermissions() {
        return metadata.getPermissions();
    }

    @Override
    public void setPermissions(long permissions) {
        metadata.setPermissions(permissions);
        metadata.setLastChangeTime(new Date());
    }

    @Override
    public void flushMetadata() throws IOException {
        metadataDao.update(metadata);
    }

    @Override
    public FileMetadata getMetadata() {
        return metadata;
    }

    @Override
    public Content getContent() {
        return content;
    }

    private class MetadataUpdaterByteChannel implements FlushableByteChannel {

        private FlushableByteChannel underlyingChannel;

        private MetadataUpdaterByteChannel(FlushableByteChannel underlyingChannel) {
            this.underlyingChannel = underlyingChannel;
        }

        @Override
        public void flush() throws IOException {
            underlyingChannel.flush();
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            int readBytes = underlyingChannel.read(dst);

            metadata.setLastAccessTime(new Date());

            return readBytes;
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            int writtenBytes = underlyingChannel.write(src);
            Date now = new Date();

            metadata.setLastChangeTime(now);
            metadata.setLastModifiedTime(now);

            return writtenBytes;
        }

        @Override
        public long position() throws IOException {
            return underlyingChannel.position();
        }

        @Override
        public SeekableByteChannel position(long newPosition) throws IOException {
            return underlyingChannel.position(newPosition);
        }

        @Override
        public long size() throws IOException {
            return underlyingChannel.size();
        }

        @Override
        public SeekableByteChannel truncate(long size) throws IOException {
            return underlyingChannel.truncate(size);
        }

        @Override
        public boolean isOpen() {
            return underlyingChannel.isOpen();
        }

        @Override
        public void close() throws IOException {
            metadataDao.update(metadata);

            underlyingChannel.close();
        }

    }

}
