package org.avasquez.seccloudfs.storage.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.erasure.DecodingException;
import org.avasquez.seccloudfs.erasure.EncodingException;
import org.avasquez.seccloudfs.erasure.ErasureDecoder;
import org.avasquez.seccloudfs.erasure.ErasureEncoder;
import org.avasquez.seccloudfs.erasure.Slices;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.storage.CloudStoreRegistry;
import org.avasquez.seccloudfs.storage.db.model.ErasureInfo;
import org.avasquez.seccloudfs.storage.db.model.SliceMetadata;
import org.avasquez.seccloudfs.storage.db.repos.ErasureInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * {@link org.avasquez.seccloudfs.cloud.CloudStore} implementation that uses erasure coding to distribute the data
 * across several clouds.
 *
 * @author avasquez
 */
public class DistributedCloudStore implements CloudStore {

    private static final Logger logger = LoggerFactory.getLogger(DistributedCloudStore.class);

    private String name;
    private CloudStoreRegistry cloudStoreRegistry;
    private ErasureInfoRepository erasureInfoRepository;
    private ErasureEncoder erasureEncoder;
    private ErasureDecoder erasureDecoder;
    private Executor taskExecutor;

    @Override
    public String getName() {
        return name;
    }

    @Required
    public void setName(final String name) {
        this.name = name;
    }

    @Required
    public void setCloudStoreRegistry(final CloudStoreRegistry cloudStoreRegistry) {
        this.cloudStoreRegistry = cloudStoreRegistry;
    }

    @Required
    public void setErasureInfoRepository(final ErasureInfoRepository erasureInfoRepository) {
        this.erasureInfoRepository = erasureInfoRepository;
    }

    @Required
    public void setErasureEncoder(final ErasureEncoder erasureEncoder) {
        this.erasureEncoder = erasureEncoder;
    }

    @Required
    public void setErasureDecoder(final ErasureDecoder erasureDecoder) {
        this.erasureDecoder = erasureDecoder;
    }

    @Required
    public void setTaskExecutor(final Executor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    @Override
    public long upload(final String id, final SeekableByteChannel src, final long length) throws IOException {
        Slices slices;
        try {
            logger.debug("Encoding data '{}'", id);

            slices = erasureEncoder.encode(src, (int) src.size());
        } catch (EncodingException e) {
            throw new IOException("Unable to encode data '" + id + "'", e);
        }

        SliceMetadata[] dataSliceMetadata = createSliceMetadata(slices.getDataSlices());
        SliceMetadata[] codingSliceMetadata = createSliceMetadata(slices.getCodingSlices());
        Queue<CloudStore> availableCloudStores = new ConcurrentLinkedQueue<>(cloudStoreRegistry.list());
        List<UploadTask> uploadTasks = createUploadTasks(slices.getDataSlices(), slices.getCodingSlices(),
            dataSliceMetadata, codingSliceMetadata, availableCloudStores);
        CompletionService<Long> uploadCompletionService = new ExecutorCompletionService<>(taskExecutor);

        for (UploadTask task : uploadTasks) {
            uploadCompletionService.submit(task);
        }

        long totalBytesUploaded = 0;
        int slicesUploaded = 0;

        for (int i = 0; i < uploadTasks.size(); i++) {
            try {
                Long bytesUploaded = uploadCompletionService.take().get();
                if (bytesUploaded != null) {
                    totalBytesUploaded += bytesUploaded;
                    slicesUploaded++;
                }
            } catch (Exception e) {
                logger.error("Error while trying to retrieve upload task result", e);
            }
        }

        logger.debug("Slices uploaded for data '{}': {}", id, slicesUploaded);

        if (slicesUploaded == uploadTasks.size()) {
            ErasureInfo erasureInfo;
            try {
                erasureInfo = erasureInfoRepository.findByDataId(id);
            } catch (DbException e) {
                throw new IOException("Unable to retrieve erasure info for data '" + id + "'");
            }

            if (erasureInfo == null) {
                erasureInfo = new ErasureInfo();
                erasureInfo.setDataId(id);
                erasureInfo.setSliceSize(slices.getDataSlices()[0].capacity());
                erasureInfo.setDataSliceMetadata(dataSliceMetadata);
                erasureInfo.setCodingSliceMetadata(codingSliceMetadata);

                try {
                    erasureInfoRepository.save(erasureInfo);
                } catch (DbException e) {
                    throw new IOException("Unable to save erasure info for data '" + id + "'");
                }
            } else {
                ErasureInfo newErasureInfo = new ErasureInfo();
                newErasureInfo.setId(erasureInfo.getId());
                newErasureInfo.setDataId(id);
                newErasureInfo.setSliceSize(slices.getDataSlices()[0].capacity());
                newErasureInfo.setDataSliceMetadata(dataSliceMetadata);
                newErasureInfo.setCodingSliceMetadata(codingSliceMetadata);

                try {
                    erasureInfoRepository.save(newErasureInfo);
                } catch (DbException e) {
                    throw new IOException("Unable to save erasure info for data '" + id + "'");
                }

                // Delete the old slices, but just after the new erasure info has been saved, so that no
                // data is lost
                deleteSlices(erasureInfo);
            }

            return totalBytesUploaded;
        } else {
            throw new IOException("Some slices for data '" + id + "' couldn't be uploaded");
        }
    }

    @Override
    public long download(final String id, final SeekableByteChannel target) throws IOException {
        ErasureInfo erasureInfo;
        try {
            erasureInfo = erasureInfoRepository.findByDataId(id);
            if (erasureInfo == null) {
                throw new IOException("No erasure info found for data '" + id + "'");
            }
        } catch (DbException e) {
            throw new IOException("Unable to retrieve erasure info for data '" + id + "'");
        }

        Queue<DownloadTask> downloadTasks = new LinkedList<>(createDownloadTasks(erasureInfo));
        CompletionService<DownloadResult> downloadCompletionService = new ExecutorCompletionService<>(taskExecutor);
        int requiredNumSlices = erasureInfo.getDataSliceMetadata().length;

        // Submit the main tasks (number of main tasks = required fragment number).
        for (int i = 0; i < requiredNumSlices; i++) {
            downloadCompletionService.submit(downloadTasks.remove());
        }

        // Keep polling for slices until we reach the required number. If a slice couldn't be loaded, try with a
        // backup task. If there are no more backup tasks, then stop.
        ByteBuffer[] dataSlices = new ByteBuffer[erasureInfo.getDataSliceMetadata().length];
        ByteBuffer[] codingSlices = new ByteBuffer[erasureInfo.getCodingSliceMetadata().length];
        long totalBytesUploaded = 0;
        int slicesDownloaded = 0;

        while (slicesDownloaded < requiredNumSlices) {
            DownloadResult result = null;
            try {
                result = downloadCompletionService.take().get();
                if (result != null) {
                    totalBytesUploaded += result.getSlice().capacity();
                    slicesDownloaded++;

                    if (result.isDataSlice()) {
                        dataSlices[result.getSliceIndex()] = result.getSlice();
                    } else {
                        codingSlices[result.getSliceIndex()] = result.getSlice();
                    }
                }
            } catch (Exception e) {
                logger.error("Error while trying to retrieve load task result", e);
            }

            if (result == null) {
                DownloadTask task = downloadTasks.poll();
                if (task != null) {
                    downloadCompletionService.submit(task);
                } else {
                    throw new IOException("Not enough slices to download");
                }
            }
        }

        logger.debug("Slices downloaded for data '{}': {}", id, slicesDownloaded);

        try {
            logger.debug("Decoding data '{}'", id);

            erasureDecoder.decode(new Slices(dataSlices, codingSlices), (int) target.size(), target);
        } catch (DecodingException e) {
            throw new IOException("Unable to decode data '" + id + "'", e);
        }

        return totalBytesUploaded;
    }

    @Override
    public void delete(final String id) throws IOException {
        ErasureInfo erasureInfo;
        try {
            erasureInfo = erasureInfoRepository.findByDataId(id);
            if (erasureInfo == null) {
                throw new IOException("No erasure info found for data '" + id + "'");
            }
        } catch (DbException e) {
            throw new IOException("Unable to retrieve erasure info for data '" + id + "'", e);
        }

        try {
            erasureInfoRepository.delete(erasureInfo.getId().toString());
        } catch (DbException e) {
            throw new IOException("Unable to delete erasure info for data '" + id + "'", e);
        }

        deleteSlices(erasureInfo);
    }

    @Override
    public long getTotalSpace() throws IOException {
        long totalSpace = 0;

        for (CloudStore store : cloudStoreRegistry.list()) {
            totalSpace += store.getTotalSpace();
        }

        return totalSpace;
    }

    @Override
    public long getAvailableSpace() throws IOException {
        long availableSpace = 0;

        for (CloudStore store : cloudStoreRegistry.list()) {
            availableSpace += store.getAvailableSpace();
        }

        return availableSpace;
    }

    private void deleteSlices(ErasureInfo erasureInfo) throws IOException {
        List<DeleteTask> deleteTasks = createDeleteTasks(erasureInfo);
        CompletionService<Boolean> deleteCompletionService = new ExecutorCompletionService<>(taskExecutor);

        for (DeleteTask task : deleteTasks) {
            deleteCompletionService.submit(task);
        }

        int slicesDeleted = 0;

        for (int i = 0; i < deleteTasks.size(); i++) {
            boolean deleted;
            try {
                deleted = deleteCompletionService.take().get();
                if (deleted) {
                    slicesDeleted++;
                }
            } catch (Exception e) {
                logger.error("Error while trying to retrieve delete task result", e);
            }
        }

        logger.debug("Slices deleted for data '{}': {}", erasureInfo.getDataId(), slicesDeleted);
    }

    private SliceMetadata[] createSliceMetadata(ByteBuffer[] slices) {
        SliceMetadata[] sliceMetadata = new SliceMetadata[slices.length];

        for (int i = 0; i < slices.length; i++) {
            SliceMetadata metadata = new SliceMetadata();
            metadata.setId(SliceMetadata.generateId());

            sliceMetadata[i] = metadata;
        }

        return sliceMetadata;
    }

    private List<UploadTask> createUploadTasks(ByteBuffer[] dataSlices, ByteBuffer[] codingSlices,
                                               SliceMetadata[] dataSliceMetadata, SliceMetadata[] codingSliceMetadata,
                                               Queue<CloudStore> availableCloudStores) {
        List<UploadTask> tasks = new ArrayList<>();

        for (int i = 0; i < dataSlices.length; i++) {
            tasks.add(new UploadTask(dataSlices[i], dataSliceMetadata[i], availableCloudStores));
        }

        for (int i = 0; i < codingSlices.length; i++) {
            tasks.add(new UploadTask(codingSlices[i], codingSliceMetadata[i], availableCloudStores));
        }

        return tasks;
    }

    private List<DownloadTask> createDownloadTasks(ErasureInfo erasureInfo) throws IOException {
        List<DownloadTask> tasks = new ArrayList<>();
        SliceMetadata[] dataSliceMetadata = erasureInfo.getDataSliceMetadata();
        SliceMetadata[] codingSliceMetadata = erasureInfo.getCodingSliceMetadata();

        for (int i = 0; i < dataSliceMetadata.length; i++) {
            CloudStore cloudStore = getCloudStoreByName(dataSliceMetadata[i].getCloudStoreName());

            tasks.add(new DownloadTask(dataSliceMetadata[i], i, true, erasureInfo.getSliceSize(), cloudStore));
        }

        for (int i = 0; i < codingSliceMetadata.length; i++) {
            CloudStore cloudStore = getCloudStoreByName(codingSliceMetadata[i].getCloudStoreName());

            tasks.add(new DownloadTask(codingSliceMetadata[i], i, false, erasureInfo.getSliceSize(), cloudStore));
        }

        return tasks;
    }

    private List<DeleteTask> createDeleteTasks(ErasureInfo erasureInfo) throws IOException {
        List<DeleteTask> tasks = new ArrayList<>();
        SliceMetadata[] dataSliceMetadata = erasureInfo.getDataSliceMetadata();
        SliceMetadata[] codingSliceMetadata = erasureInfo.getCodingSliceMetadata();

        for (int i = 0; i < dataSliceMetadata.length; i++) {
            CloudStore cloudStore = getCloudStoreByName(dataSliceMetadata[i].getCloudStoreName());

            tasks.add(new DeleteTask(dataSliceMetadata[i], cloudStore));
        }

        for (int i = 0; i < codingSliceMetadata.length; i++) {
            CloudStore cloudStore = getCloudStoreByName(codingSliceMetadata[i].getCloudStoreName());

            tasks.add(new DeleteTask(codingSliceMetadata[i], cloudStore));
        }

        return tasks;
    }

    private CloudStore getCloudStoreByName(String name) throws IOException {
        CloudStore cloudStore = cloudStoreRegistry.find(name);
        if (cloudStore == null) {
            return cloudStore;
        } else {
            throw new IOException("No cloud store found for name '" + name + "'");
        }
    }

}
