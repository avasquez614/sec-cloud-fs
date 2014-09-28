package org.avasquez.seccloudfs.processing.impl;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;

import org.apache.commons.lang3.ArrayUtils;
import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.cloud.CloudStoreRegistry;
import org.avasquez.seccloudfs.erasure.DecodingException;
import org.avasquez.seccloudfs.erasure.EncodingException;
import org.avasquez.seccloudfs.erasure.ErasureDecoder;
import org.avasquez.seccloudfs.erasure.ErasureEncoder;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.processing.db.model.ErasureInfo;
import org.avasquez.seccloudfs.processing.db.model.SliceMetadata;
import org.avasquez.seccloudfs.processing.db.repos.ErasureInfoRepository;
import org.avasquez.seccloudfs.utils.FileUtils;
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

    private static final String SLICE_FILE_SUFFIX = ".slice";

    private CloudStoreRegistry cloudStoreRegistry;
    private ErasureInfoRepository erasureInfoRepository;
    private ErasureEncoder erasureEncoder;
    private ErasureDecoder erasureDecoder;
    private Executor taskExecutor;
    private Path tmpDir;

    @Override
    public String getName() {
        return DistributedCloudStore.class.getSimpleName();
    }

    @Required
    public void setCloudStoreRegistry(CloudStoreRegistry cloudStoreRegistry) {
        this.cloudStoreRegistry = cloudStoreRegistry;
    }

    @Required
    public void setErasureInfoRepository(ErasureInfoRepository erasureInfoRepository) {
        this.erasureInfoRepository = erasureInfoRepository;
    }

    @Required
    public void setErasureEncoder(ErasureEncoder erasureEncoder) {
        this.erasureEncoder = erasureEncoder;
    }

    @Required
    public void setErasureDecoder(ErasureDecoder erasureDecoder) {
        this.erasureDecoder = erasureDecoder;
    }

    @Required
    public void setTaskExecutor(Executor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    @Required
    public void setTmpDir(String tmpDir) {
        this.tmpDir = Paths.get(tmpDir);
    }

    @Override
    public void upload(String id, ReadableByteChannel src, long length) throws IOException {
        FileChannel[] dataSlices = null;
        FileChannel[] codingSlices = null;

        try {
            dataSlices = createSliceFiles(erasureEncoder.getK());
            codingSlices = createSliceFiles(erasureEncoder.getM());

            int sliceSize;

            try {
                logger.debug("Encoding data '{}'", id);

                sliceSize = erasureEncoder.encode(src, (int) length, dataSlices, codingSlices);
            } catch (EncodingException e) {
                throw new IOException("Unable to encode data '" + id + "'", e);
            }

            resetChannels(dataSlices);
            resetChannels(codingSlices);

            SliceMetadata[] dataSliceMetadata = createSliceMetadata(dataSlices);
            SliceMetadata[] codingSliceMetadata = createSliceMetadata(codingSlices);
            Queue<CloudStore> availableCloudStores = new ConcurrentLinkedQueue<>(cloudStoreRegistry.list());
            List<UploadTask> uploadTasks = createUploadTasks(dataSlices, codingSlices, dataSliceMetadata,
                codingSliceMetadata, sliceSize, availableCloudStores);
            CompletionService<Boolean> uploadCompletionService = new ExecutorCompletionService<>(taskExecutor);

            for (UploadTask task : uploadTasks) {
                uploadCompletionService.submit(task);
            }

            int slicesUploaded = 0;

            for (int i = 0; i < uploadTasks.size(); i++) {
                try {
                    boolean uploaded = uploadCompletionService.take().get();
                    if (uploaded) {
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
                    erasureInfo.setDataSize((int) length);
                    erasureInfo.setDataSliceMetadata(dataSliceMetadata);
                    erasureInfo.setCodingSliceMetadata(codingSliceMetadata);

                    try {
                        erasureInfoRepository.insert(erasureInfo);
                    } catch (DbException e) {
                        throw new IOException("Unable to save erasure info for data '" + id + "'");
                    }
                } else {
                    ErasureInfo newErasureInfo = new ErasureInfo();
                    newErasureInfo.setId(erasureInfo.getId());
                    newErasureInfo.setDataId(id);
                    newErasureInfo.setDataSize((int) length);
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
            } else {
                throw new IOException("Some slices for data '" + id + "' couldn't be uploaded");
            }
        } finally {
            closeChannels(dataSlices);
            closeChannels(codingSlices);
        }
    }

    @Override
    public void download(String id, WritableByteChannel target) throws IOException {
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
        FileChannel[] dataSlices = new FileChannel[erasureInfo.getDataSliceMetadata().length];
        FileChannel[] codingSlices = new FileChannel[erasureInfo.getCodingSliceMetadata().length];

        try {
            int slicesDownloaded = 0;

            while (slicesDownloaded < requiredNumSlices) {
                DownloadResult result = null;
                try {
                    result = downloadCompletionService.take().get();
                    if (result != null) {
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
                        throw new IOException("Not enough slices could be downloaded to reconstruct data " + id);
                    }
                }
            }

            logger.debug("Slices downloaded for data '{}': {}", id, slicesDownloaded);

            resetChannels(dataSlices);
            resetChannels(codingSlices);

            try {
                logger.debug("Decoding data '{}'", id);

                erasureDecoder.decode(erasureInfo.getDataSize(), dataSlices, codingSlices, target);
            } catch (DecodingException e) {
                throw new IOException("Unable to decode data '" + id + "'", e);
            }
        } finally {
            closeChannels(dataSlices);
            closeChannels(codingSlices);
        }
    }

    @Override
    public void delete(String id) throws IOException {
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

    private FileChannel[] createSliceFiles(int num) throws IOException {
        FileChannel[] channels = new FileChannel[num];

        for (int i = 0; i < num; i++) {
            Path tmpFile = Files.createTempFile(tmpDir, null, SLICE_FILE_SUFFIX);
            channels[i] = FileChannel.open(tmpFile, FileUtils.TMP_FILE_OPEN_OPTIONS);
        }

        return channels;
    }

    private void resetChannels(FileChannel[] channels) throws IOException {
        if (ArrayUtils.isNotEmpty(channels)) {
            for (FileChannel channel : channels) {
                if (channel != null) {
                    channel.position(0);
                }
            }
        }
    }

    private void closeChannels(FileChannel[] channels) {
        if (ArrayUtils.isNotEmpty(channels)) {
            for (FileChannel channel : channels) {
                if (channel != null) {
                    try {
                        channel.close();
                    } catch (IOException e) {
                        logger.trace("Unable to close channel", e);
                    }
                }
            }
        }
    }

    private SliceMetadata[] createSliceMetadata(ReadableByteChannel[] slices) {
        SliceMetadata[] sliceMetadata = new SliceMetadata[slices.length];

        for (int i = 0; i < slices.length; i++) {
            SliceMetadata metadata = new SliceMetadata();
            metadata.setId(SliceMetadata.generateId());

            sliceMetadata[i] = metadata;
        }

        return sliceMetadata;
    }

    private List<UploadTask> createUploadTasks(ReadableByteChannel[] dataSlices, ReadableByteChannel[] codingSlices,
                                               SliceMetadata[] dataSliceMetadata, SliceMetadata[] codingSliceMetadata,
                                               int sliceSize, Queue<CloudStore> availableCloudStores) {
        List<UploadTask> tasks = new ArrayList<>();

        for (int i = 0; i < dataSlices.length; i++) {
            tasks.add(new UploadTask(dataSlices[i], sliceSize, dataSliceMetadata[i], availableCloudStores));
        }

        for (int i = 0; i < codingSlices.length; i++) {
            tasks.add(new UploadTask(codingSlices[i], sliceSize, codingSliceMetadata[i], availableCloudStores));
        }

        return tasks;
    }

    private List<DownloadTask> createDownloadTasks(ErasureInfo erasureInfo) throws IOException {
        List<DownloadTask> tasks = new ArrayList<>();
        SliceMetadata[] dataSliceMetadata = erasureInfo.getDataSliceMetadata();
        SliceMetadata[] codingSliceMetadata = erasureInfo.getCodingSliceMetadata();

        for (int i = 0; i < dataSliceMetadata.length; i++) {
            CloudStore cloudStore = getCloudStoreByName(dataSliceMetadata[i].getCloudStoreName());
            Path sliceFile = Files.createTempFile(tmpDir, dataSliceMetadata[i].getId(), SLICE_FILE_SUFFIX);

            tasks.add(new DownloadTask(dataSliceMetadata[i], i, true, cloudStore, sliceFile));
        }

        for (int i = 0; i < codingSliceMetadata.length; i++) {
            CloudStore cloudStore = getCloudStoreByName(codingSliceMetadata[i].getCloudStoreName());
            Path sliceFile = Files.createTempFile(tmpDir, dataSliceMetadata[i].getId(), SLICE_FILE_SUFFIX);

            tasks.add(new DownloadTask(codingSliceMetadata[i], i, false, cloudStore, sliceFile));
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
        if (cloudStore != null) {
            return cloudStore;
        } else {
            throw new IOException("No cloud store found for name '" + name + "'");
        }
    }

}
