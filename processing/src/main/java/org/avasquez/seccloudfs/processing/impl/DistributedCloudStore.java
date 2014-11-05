package org.avasquez.seccloudfs.processing.impl;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.cloud.CloudStoreRegistry;
import org.avasquez.seccloudfs.erasure.DecodingException;
import org.avasquez.seccloudfs.erasure.EncodingException;
import org.avasquez.seccloudfs.erasure.ErasureDecoder;
import org.avasquez.seccloudfs.erasure.ErasureEncoder;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.processing.db.model.SliceMetadata;
import org.avasquez.seccloudfs.processing.db.model.Upload;
import org.avasquez.seccloudfs.processing.db.repos.UploadRepository;
import org.avasquez.seccloudfs.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;

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
    private UploadRepository uploadRepository;
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
    public void setUploadRepository(UploadRepository uploadRepository) {
        this.uploadRepository = uploadRepository;
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
        int k = erasureEncoder.getK();
        int m = erasureEncoder.getM();

        try {
            dataSlices = createSliceFiles(k);
            codingSlices = createSliceFiles(m);

            int sliceSize;

            try {
                logger.debug("Encoding data '{}' with k = {} and m = {}", id, k, m);

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

            Upload upload = new Upload();
            upload.setDataId(id);
            upload.setDataSize((int) length);
            upload.setFinishDate(new Date());
            upload.setDataSliceMetadata(dataSliceMetadata);
            upload.setCodingSliceMetadata(codingSliceMetadata);

            if (slicesUploaded == uploadTasks.size()) {
                Upload lastUpload;
                try {
                    lastUpload = uploadRepository.findLastSuccessfulByDataId(id);
                } catch (DbException e) {
                    throw new IOException("Unable to retrieve upload for data '" + id + "' from DB");
                }

                upload.setSuccess(true);

                try {
                    uploadRepository.insert(upload);
                } catch (DbException e) {
                    throw new IOException("Unable to save upload for data '" + id + "' to DB");
                }

                if (lastUpload != null) {
                    // Delete the last upload, but just after the new one has been saved, so that no data is lost
                    deleteUpload(lastUpload);
                }
            } else {
                upload.setSuccess(false);

                try {
                    uploadRepository.insert(upload);
                } catch (DbException e) {
                    throw new IOException("Unable to save upload for data '" + id + "' to DB");
                }

                logger.error("Upload '{}' for data '{}' failed. Trying to rollback...", upload.getId(), id);

                deleteUpload(upload);

                throw new IOException("Upload '" + upload.getId() + "' for data '" + id + "' failed");
            }
        } finally {
            closeChannels(dataSlices);
            closeChannels(codingSlices);
        }
    }

    @Override
    public void download(String id, WritableByteChannel target) throws IOException {
        Upload upload;
        try {
            upload = uploadRepository.findLastSuccessfulByDataId(id);
            if (upload == null) {
                throw new IOException("No last successful upload found for data '" + id + "' in DB");
            }
        } catch (DbException e) {
            throw new IOException("Unable to retrieve last successful upload found for data '" + id + "' from DB", e);
        }

        Queue<DownloadTask> downloadTasks = new LinkedList<>(createDownloadTasks(upload));
        CompletionService<DownloadResult> downloadCompletionService = new ExecutorCompletionService<>(taskExecutor);
        int requiredNumSlices = erasureDecoder.getK();

        // Submit the main tasks (number of main tasks = required fragment number).
        for (int i = 0; i < requiredNumSlices; i++) {
            downloadCompletionService.submit(downloadTasks.remove());
        }

        // Keep polling for slices until we reach the required number. If a slice couldn't be loaded, try with a
        // backup task. If there are no more backup tasks, then stop.
        FileChannel[] dataSlices = new FileChannel[upload.getDataSliceMetadata().length];
        FileChannel[] codingSlices = new FileChannel[upload.getCodingSliceMetadata().length];

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

                erasureDecoder.decode(upload.getDataSize(), dataSlices, codingSlices, target);
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
        Upload upload;
        try {
            upload = uploadRepository.findLastSuccessfulByDataId(id);
            if (upload == null) {
                throw new IOException("No last successful upload found for data '" + id + "' in DB");
            }
        } catch (DbException e) {
            throw new IOException("Unable to retrieve last successful upload found for data '" + id + "' from DB", e);
        }

        deleteUpload(upload);
    }

    private void deleteUpload(Upload upload) throws IOException {
        List<DeleteTask> deleteTasks = createDeleteTasks(upload);
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

        logger.debug("Slices deleted for upload '{}': {}", upload.getId(), slicesDeleted);

        try {
            uploadRepository.delete(upload.getId());
        } catch (DbException e) {
            throw new IOException("Unable to delete upload " + upload.getId() + " from DB", e);
        }
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

    private List<DownloadTask> createDownloadTasks(Upload upload) throws IOException {
        List<DownloadTask> tasks = new ArrayList<>();
        SliceMetadata[] dataSliceMetadata = upload.getDataSliceMetadata();
        SliceMetadata[] codingSliceMetadata = upload.getCodingSliceMetadata();

        for (int i = 0; i < dataSliceMetadata.length; i++) {
            createDownloadTask(dataSliceMetadata[i], i, true, tasks);
        }

        for (int i = 0; i < codingSliceMetadata.length; i++) {
            createDownloadTask(codingSliceMetadata[i], i, true, tasks);
        }

        return tasks;
    }

    private List<DeleteTask> createDeleteTasks(Upload upload) throws IOException {
        List<DeleteTask> tasks = new ArrayList<>();
        SliceMetadata[] dataSliceMetadata = upload.getDataSliceMetadata();
        SliceMetadata[] codingSliceMetadata = upload.getCodingSliceMetadata();

        for (int i = 0; i < dataSliceMetadata.length; i++) {
            createDeleteTask(dataSliceMetadata[i], tasks);
        }

        for (int i = 0; i < codingSliceMetadata.length; i++) {
            createDeleteTask(codingSliceMetadata[i], tasks);
        }

        return tasks;
    }

    private void createDownloadTask(SliceMetadata sliceMetadata, int sliceIdx, boolean dataSlice,
                                    List<DownloadTask> tasks) throws IOException {
        String cloudStoreName = sliceMetadata.getCloudStoreName();
        if (StringUtils.isNotEmpty(cloudStoreName)) {
            CloudStore cloudStore = getCloudStoreByName(cloudStoreName);
            Path sliceFile = Files.createTempFile(tmpDir, sliceMetadata.getId(), SLICE_FILE_SUFFIX);

            tasks.add(new DownloadTask(sliceMetadata, sliceIdx, dataSlice, cloudStore, sliceFile));
        }
    }

    private void createDeleteTask(SliceMetadata sliceMetadata, List<DeleteTask> tasks) throws IOException {
        String cloudStoreName = sliceMetadata.getCloudStoreName();
        if (StringUtils.isNotEmpty(cloudStoreName)) {
            CloudStore cloudStore = getCloudStoreByName(cloudStoreName);

            tasks.add(new DeleteTask(sliceMetadata, cloudStore));
        }
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
