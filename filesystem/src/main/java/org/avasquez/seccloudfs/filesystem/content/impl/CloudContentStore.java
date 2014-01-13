package org.avasquez.seccloudfs.filesystem.content.impl;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.filesystem.content.Content;
import org.avasquez.seccloudfs.filesystem.db.dao.ContentMetadataDao;
import org.avasquez.seccloudfs.filesystem.db.model.ContentMetadata;

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
    protected Content doFind(String id) {
        ContentMetadata metadata = metadataDao.find(id);
        if (metadata != null) {
            return createContentObject(metadata);
        } else {
            return null;
        }
    }

    @Override
    protected Content doCreate() {
        ContentMetadata metadata = new ContentMetadata();
        metadataDao.insert(metadata);

        return createContentObject(metadata);
    }

    @Override
    protected void doDelete(String id) {
        metadataDao.delete(id);
    }

    private Content createContentObject(ContentMetadata metadata) {
        Path contentPath = Paths.get(cacheContentDir, metadata.getId());
        ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        Downloader downloader = new Downloader(metadata.getId(), contentPath, cloudStore);
        Uploader uploader = new Uploader(metadata, metadataDao, contentPath, timeoutForNextUpdate, threadPool,
                readWriteLock, cloudStore);

        return new CloudContent(metadata, contentPath, downloader, uploader, readWriteLock);
    }

}
