package org.avasquez.seccloudfs.filesystem.files.impl;

import org.avasquez.seccloudfs.filesystem.content.Content;
import org.avasquez.seccloudfs.filesystem.db.dao.FileMetadataDao;
import org.avasquez.seccloudfs.filesystem.db.model.DirEntry;
import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;
import org.avasquez.seccloudfs.filesystem.files.File;
import org.avasquez.seccloudfs.filesystem.files.FileStore;
import org.avasquez.seccloudfs.filesystem.files.User;
import org.avasquez.seccloudfs.filesystem.util.FlushableByteChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by alfonsovasquez on 19/01/14.
 */
public class FileImpl implements MetadataAwareFile, ContentAwareFile {

    private FileStore fileStore;
    private FileMetadata metadata;
    private FileMetadataDao metadataDao;
    private Content content;

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
    public boolean isEmpty() throws IOException {
        Map<String, DirEntry> dirEntries = getDirEntries();
        if (dirEntries != null) {
            return dirEntries.isEmpty();
        }

        return true;
    }

    @Override
    public File getChild(String name) throws IOException {
        Map<String, DirEntry> dirEntries = getDirEntries();
        if (dirEntries != null) {
            DirEntry entry = dirEntries.get(name);
            if (entry != null) {
                return fileStore.find(entry.getFileId());
            }
        }

        return null;
    }

    @Override
    public String getChildName(String fileId) throws IOException {
        Map<String, DirEntry> dirEntries = getDirEntries();
        if (dirEntries != null) {
            for (Map.Entry<String, DirEntry> entry : dirEntries.entrySet()) {
                if (entry.getValue().getFileId().equals(fileId)) {
                    return entry.getKey();
                }
            }
        }

        return null;
    }

    @Override
    public boolean hasChild(String name) throws IOException {
        Map<String, DirEntry> dirEntries = getDirEntries();
        if (dirEntries != null) {
            dirEntries.containsKey(name);
        }

        return false;
    }

    @Override
    public String[] getChildren() throws IOException {
        Map<String, DirEntry> dirEntries = getDirEntries();
        if (dirEntries != null) {
            Set<String> childNames = dirEntries.keySet();

            return childNames.toArray(new String[childNames.size()]);
        }

        return null;
    }

    @Override
    public void addChild(String name, String fileId) throws IOException {
        Map<String, DirEntry> dirEntries = getDirEntries();
        if (dirEntries != null) {
            dirEntries.put(name, new DirEntry(fileId, new Date()));

            metadataDao.update(metadata);
        }
    }

    @Override
    public void removeChild(String name) throws IOException {
        Map<String, DirEntry> dirEntries = getDirEntries();
        if (dirEntries != null) {
            dirEntries.remove(name);

            metadataDao.update(metadata);
        }
    }

    @Override
    public void removeChildById(String id) throws IOException {
        Map<String, DirEntry> dirEntries = getDirEntries();
        if (dirEntries != null) {
            for (Iterator<DirEntry> iter = dirEntries.values().iterator(); iter.hasNext();) {
                if (iter.next().getFileId().equals(id)) {
                    iter.remove();

                    metadataDao.update(metadata);
                }
            }
        }
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FileImpl file = (FileImpl) o;

        if (!metadata.equals(file.metadata)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return metadata.hashCode();
    }

    private Map<String, DirEntry> getDirEntries() throws IOException {
        if (isDirectory() && metadata.getDirEntries() == null) {
            synchronized (this) {
                if (metadata.getDirEntries() == null) {
                    metadata.setDirEntries(new ConcurrentHashMap<String, DirEntry>());
                }
            }
        }

        return metadata.getDirEntries();
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
