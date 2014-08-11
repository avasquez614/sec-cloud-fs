package org.avasquez.seccloudfs.storage.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
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

    private CloudStoreRegistry cloudStoreRegistry;
    private ErasureInfoRepository erasureInfoRepository;
    private ErasureEncoder erasureEncoder;
    private ErasureDecoder erasureDecoder;
    private Executor taskExecutor;

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
            slices = erasureEncoder.encode(src, (int) src.size());
        } catch (EncodingException e) {
            throw new IOException("Unable to encode data '" + id + "'", e);
        }

        Queue<CloudStore> availableCloudStores = new ConcurrentLinkedQueue<>(cloudStoreRegistry.list());
        List<SliceUploadTask> uploadTasks = createUploadTasks(id, slices, availableCloudStores);
        CompletionService<Long> uploadCompletionService = new ExecutorCompletionService<>(taskExecutor);

        for (SliceUploadTask task : uploadTasks) {
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

        if (slicesUploaded == uploadTasks.size()) {
            return totalBytesUploaded;
        } else {
            throw new IOException("Some slices for data '" + id + "' couldn't be uploaded");
        }
    }

    @Override
    public long download(final String id, final SeekableByteChannel target) throws IOException {
        Iterable<SliceMetadata> metadataList;
        try {
            metadataList = erasureInfoRepository.findByDataId(id);
        } catch (DbException e) {
            throw new IOException("Unable to retrieve slice metadata for data '" + id + "'");
        }

        List<CloudStore> cloudStoresUsed = new ArrayList<>();
        for (Slice)
    }

    @Override
    public void delete(final String id) throws IOException {

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

    private Slices encode(SeekableByteChannel src) throws IOException {
        try {
            return erasureEncoder.encode(src, (int) src.size());
        } catch (EncodingException e) {
            throw new IOException("Unable to encode data", e);
        }
    }

    private void decode(Slices slices, SeekableByteChannel target) throws IOException {
        try {
            erasureDecoder.decode(slices, (int) target.size(), target);
        } catch (DecodingException e) {
            throw new IOException("Unable to decode data", e);
        }
    }

    private List<SliceUploadTask> createUploadTasks(String dataId, Slices slices,
                                                    Queue<CloudStore> availableCloudStores) {
        List<SliceUploadTask> tasks = new ArrayList<>();
        ByteBuffer[] dataSlices = slices.getDataSlices();
        ByteBuffer[] codingSlices = slices.getCodingSlices();

        for (int i = 0; i < dataSlices.length; i++) {
            SliceMetadata metadata = new SliceMetadata();
            metadata.setId(SliceMetadata.generateId());
            metadata.setDataId(dataId);
            metadata.setType(SliceMetadata.SliceType.DATA);
            metadata.setIndex(i);
            metadata.setSize(dataSlices[i].capacity());

            tasks.add(new SliceUploadTask(dataSlices[i], metadata, erasureInfoRepository, availableCloudStores));
        }

        for (int i = 0; i < codingSlices.length; i++) {
            SliceMetadata metadata = new SliceMetadata();
            metadata.setId(SliceMetadata.generateId());
            metadata.setDataId(dataId);
            metadata.setType(SliceMetadata.SliceType.CODING);
            metadata.setIndex(i);
            metadata.setSize(codingSlices[i].capacity());

            tasks.add(new SliceUploadTask(codingSlices[i], metadata, erasureInfoRepository, availableCloudStores));
        }

        return tasks;
    }

    private List<SliceDownloadTask> createDownloadTasks(Iterable<SliceMetadata> metadata) throws IOException {
        List<SliceMetadata> dataSliceMetadata = new ArrayList<>();
        List<SliceMetadata>

        for (SliceMetadata m : metadata) {

        }
    }

}
