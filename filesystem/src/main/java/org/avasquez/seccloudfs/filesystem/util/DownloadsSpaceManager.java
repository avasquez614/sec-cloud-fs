package org.avasquez.seccloudfs.filesystem.util;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.filesystem.content.CloudContent;
import org.avasquez.seccloudfs.filesystem.content.ContentStore;
import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;
import org.avasquez.seccloudfs.filesystem.db.repos.FileMetadataRepository;
import org.avasquez.seccloudfs.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Handles the space used by the downloads directory. The downloads dir has a max size. When the max size has been
 * reached, the LRU content is deleted until the dir size is again less than the max size.
 *
 * @author avasquez
 */
public class DownloadsSpaceManager {

    private static final Logger logger = LoggerFactory.getLogger(DownloadsSpaceManager.class);

    private Path downloadsDir;
    private long maxDirSize;
    private FileMetadataRepository fileMetadataRepo;
    private ContentStore contentStore;

    @Required
    public void setDownloadsDir(String downloadsDir) {
        this.downloadsDir = Paths.get(downloadsDir);
    }

    @Required
    public void setMaxDirSize(String maxDirSize) {
        this.maxDirSize = FileUtils.humanReadableByteSizeToByteCount(maxDirSize);
    }

    @Required
    public void setFileMetadataRepo(FileMetadataRepository fileMetadataRepo) {
        this.fileMetadataRepo = fileMetadataRepo;
    }

    @Required
    public void setContentStore(ContentStore contentStore) {
        this.contentStore = contentStore;
    }

    @Scheduled(fixedDelayString = "${cloud.content.downloads.dir.maxSize.checkDelayMillis}")
    public void checkMaxSize() {
        long dirSize;
        try {
            dirSize = FileUtils.sizeOfDirectory(downloadsDir);
        } catch (IOException e) {
            logger.error("Unable to calculate size of downloads dir " + downloadsDir, e);

            return;
        }

        if (dirSize > maxDirSize) {
            String hrDirSize = FileUtils.byteCountToHumanReadableByteSize(dirSize);
            String hrMaxDirSize = FileUtils.byteCountToHumanReadableByteSize(maxDirSize);

            logger.info("Downloads dir {} max size {} reached (current size {})", downloadsDir, hrMaxDirSize, hrDirSize);

            Iterable<FileMetadata> lruMetadata;
            try {
                lruMetadata = fileMetadataRepo.findFilesSortedByLastAccessTime();
            } catch (DbException e) {
                logger.error("Error while retrieving file metadata sorted by desc last access time", e);

                return;
            }

            Iterator<FileMetadata> lruIter = lruMetadata.iterator();
            while (dirSize > maxDirSize && lruIter.hasNext()){
                FileMetadata fileMetadata = lruIter.next();
                String contentId = fileMetadata.getContentId();

                logger.debug("Trying to delete content '{}' (if it has been downloaded)", contentId);

                try {
                    CloudContent content = (CloudContent) contentStore.find(contentId);
                    long size = content.getSize();
                    boolean deleted = content.deleteDownload();

                    if (deleted) {
                        dirSize -= size;
                    }
                } catch (IOException e) {
                    logger.error("Unable to delete download for content '" + contentId + "'", e);
                }
            }

            hrDirSize = FileUtils.byteCountToHumanReadableByteSize(dirSize);

            if (dirSize <= maxDirSize) {
                logger.info("Downloads dir {} was shrunk successfully to size {}", downloadsDir, hrDirSize);
            } else {
                logger.warn("Downloads dir {} couldn't be shrunk successfully to less than max size {}. " +
                        "Current size is {}", downloadsDir, hrMaxDirSize, hrDirSize);
            }
        }
    }

}
