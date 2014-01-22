package org.avasquez.seccloudfs.filesystem.content.impl;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.filesystem.content.Content;
import org.avasquez.seccloudfs.filesystem.db.dao.ContentMetadataDao;
import org.avasquez.seccloudfs.filesystem.db.model.ContentMetadata;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by alfonsovasquez on 12/01/14.
 */
public class CloudContentStore extends AbstractCachedContentStore {

    private ContentMetadataDao metadataDao;
    private CloudStore cloudStore;
    private String cacheContentDir;
    private long timeoutForNextUpdate;
    private Executor threadPool;

    public void setMetadataDao(ContentMetadataDao metadataDao) {
        this.metadataDao = metadataDao;
    }

    public void setCloudStore(CloudStore cloudStore) {
        this.cloudStore = cloudStore;
    }

    public void setCacheContentDir(String cacheContentDir) {
        this.cacheContentDir = cacheContentDir;
    }

    public void setTimeoutForNextUpdate(long timeoutForNextUpdate) {
        this.timeoutForNextUpdate = timeoutForNextUpdate;
    }

    public void setThreadPool(Executor threadPool) {
        this.threadPool = threadPool;
    }

    @Override
    protected Content doFind(String id) throws IOException {
        ContentMetadata metadata = metadataDao.find(id);
        if (metadata != null) {
            return createContentObject(metadata);
        } else {
            return null;
        }
    }

    @Override
    protected Content doCreate() throws IOException {
        ContentMetadata metadata = new ContentMetadata();
        metadataDao.insert(metadata);

        return createContentObject(metadata);
    }

    @Override
    protected void doDelete(String id) throws IOException {
        DeletableContent content = (DeletableContent) find(id);
        content.delete();
    }

    private DeletableContent createContentObject(ContentMetadata metadata) throws IOException {
        Path contentPath = Paths.get(cacheContentDir, metadata.getId());
        ReadWriteLock rwLock = new ReentrantReadWriteLock();
        Uploader uploader = new Uploader(metadata, metadataDao, contentPath, timeoutForNextUpdate,
                threadPool, rwLock, cloudStore);

        return new CloudContent(metadata, metadataDao, contentPath, cloudStore, uploader, rwLock);
    }

}
