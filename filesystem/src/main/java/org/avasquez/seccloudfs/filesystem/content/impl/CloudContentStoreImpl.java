package org.avasquez.seccloudfs.filesystem.content.impl;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.filesystem.content.Content;
import org.avasquez.seccloudfs.filesystem.db.dao.ContentMetadataDao;
import org.avasquez.seccloudfs.filesystem.db.model.ContentMetadata;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by alfonsovasquez on 12/01/14.
 */
public class CloudContentStoreImpl extends AbstractCachedContentStore {

    private ContentMetadataDao metadataDao;
    private CloudStore cloudStore;
    private Path downloadsDir;
    private Path snapshotDir;
    private long timeoutForNextUpdate;
    private ScheduledExecutorService executorService;

    public void setMetadataDao(ContentMetadataDao metadataDao) {
        this.metadataDao = metadataDao;
    }

    public void setCloudStore(CloudStore cloudStore) {
        this.cloudStore = cloudStore;
    }

    public void setDownloadsDir(String downloadsDir) {
        this.downloadsDir = Paths.get(downloadsDir);
    }

    public void setSnapshotDir(String snapshotDir) {
        this.snapshotDir = Paths.get(snapshotDir);
    }

    public void setTimeoutForNextUpdate(long timeoutForNextUpdate) {
        this.timeoutForNextUpdate = timeoutForNextUpdate;
    }

    public void setExecutorService(ScheduledExecutorService executorService) {
        this.executorService = executorService;
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
    protected void doDelete(Content content) throws IOException {
        ((CloudContentImpl) content).delete();
    }

    private Content createContentObject(ContentMetadata metadata) throws IOException {
        Path downloadPath = downloadsDir.resolve(metadata.getId());
        Lock accessLock = new ReentrantLock();
        Uploader uploader = new Uploader(metadata, metadataDao, downloadPath, accessLock, snapshotDir,
                timeoutForNextUpdate, executorService, cloudStore);

        return new CloudContentImpl(metadata, metadataDao, downloadPath, accessLock, cloudStore, uploader);
    }

}
