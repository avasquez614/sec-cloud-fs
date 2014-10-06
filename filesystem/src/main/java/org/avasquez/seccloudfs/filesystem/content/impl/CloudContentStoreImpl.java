package org.avasquez.seccloudfs.filesystem.content.impl;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.PostConstruct;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.filesystem.content.CloudContent;
import org.avasquez.seccloudfs.filesystem.content.Content;
import org.avasquez.seccloudfs.filesystem.db.model.ContentMetadata;
import org.avasquez.seccloudfs.filesystem.db.repos.ContentMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Created by alfonsovasquez on 12/01/14.
 */
public class CloudContentStoreImpl extends AbstractCachedContentStore {

    private static final Logger logger = LoggerFactory.getLogger(CloudContentStoreImpl.class);

    private ContentMetadataRepository metadataRepo;
    private CloudStore cloudStore;
    private Path downloadsDir;
    private Path snapshotDir;
    private ScheduledExecutorService executorService;
    private long timeoutForNextUpdateSecs;
    private long retryUploadDelaySecs;

    @Required
    public void setMetadataRepo(ContentMetadataRepository metadataRepo) {
        this.metadataRepo = metadataRepo;
    }

    @Required
    public void setCloudStore(CloudStore cloudStore) {
        this.cloudStore = cloudStore;
    }

    @Required
    public void setDownloadsDir(String downloadsDir) {
        this.downloadsDir = Paths.get(downloadsDir);
    }

    @Required
    public void setSnapshotDir(String snapshotDir) {
        this.snapshotDir = Paths.get(snapshotDir);
    }

    @Required
    public void setExecutorService(ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }

    @Required
    public void setTimeoutForNextUpdateSecs(long timeoutForNextUpdateSecs) {
        this.timeoutForNextUpdateSecs = timeoutForNextUpdateSecs;
    }

    @Required
    public void setRetryUploadDelaySecs(long retryUploadDelaySecs) {
        this.retryUploadDelaySecs = retryUploadDelaySecs;
    }

    @Override
    public long getTotalSpace() throws IOException {
        return cloudStore.getTotalSpace();
    }

    @Override
    public long getAvailableSpace() throws IOException {
        return cloudStore.getAvailableSpace();
    }

    @PostConstruct
    public void init() throws IOException {
        resumeUploads();
    }

    @Override
    protected Content doFind(String id) throws IOException {
        ContentMetadata metadata = findMetadata(id);
        if (metadata != null) {
            return createContentObject(metadata);
        } else {
            return null;
        }
    }

    @Override
    protected Content doCreate() throws IOException {
        ContentMetadata metadata = new ContentMetadata();
        try {
            metadataRepo.insert(metadata);
        } catch (DbException e) {
            throw new IOException("Unable to insert " + metadata + " into DB", e);
        }

        Content content = createContentObject(metadata);

        logger.debug("{} created", content);

        return content;
    }

    @Override
    protected void doDelete(Content content) throws IOException {
        ((CloudContentImpl) content).delete();
    }

    private CloudContent createContentObject(ContentMetadata metadata) throws IOException {
        Path downloadPath = downloadsDir.resolve(metadata.getId());
        Lock accessLock = new ReentrantLock();
        Uploader uploader = new Uploader(metadata, metadataRepo, cloudStore, downloadPath, accessLock,
                snapshotDir, executorService, timeoutForNextUpdateSecs, retryUploadDelaySecs);

        return new CloudContentImpl(metadata, metadataRepo, downloadPath, accessLock, cloudStore, uploader);
    }

    private ContentMetadata findMetadata(String id) throws IOException {
        try {
            return metadataRepo.find(id);
        } catch (DbException e) {
            throw new IOException("Unable to find content by ID '" + id + "'", e);
        }
    }

    private void resumeUploads() throws IOException {
        logger.info("Checking for pending uploads...");

        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(downloadsDir)) {
            for (Path file : dirStream) {
                String id = file.getFileName().toString();
                ContentMetadata metadata = findMetadata(id);
                FileTime lastUploadTime = FileTime.fromMillis(metadata.getLastUploadTime().getTime());
                FileTime lastModifiedTime = Files.getLastModifiedTime(file);

                if (lastModifiedTime.compareTo(lastUploadTime) > 0) {
                    logger.info("Upload of content '{}' pending. Restarting it...", id);

                    CloudContent content = createContentObject(metadata);
                    content.forceUpload();

                    cache.put(id, content);
                }
            }
        } catch (IOException e) {
            throw new IOException("Error resuming pending uploads", e);
        }
    }

}
