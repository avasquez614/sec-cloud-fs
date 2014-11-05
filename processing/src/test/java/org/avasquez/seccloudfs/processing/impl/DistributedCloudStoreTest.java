package org.avasquez.seccloudfs.processing.impl;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.cloud.CloudStoreRegistry;
import org.avasquez.seccloudfs.cloud.impl.CloudStoreRegistryImpl;
import org.avasquez.seccloudfs.erasure.ErasureDecoder;
import org.avasquez.seccloudfs.erasure.ErasureEncoder;
import org.avasquez.seccloudfs.processing.db.model.SliceMetadata;
import org.avasquez.seccloudfs.processing.db.model.Upload;
import org.avasquez.seccloudfs.processing.db.repos.UploadRepository;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executors;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link org.avasquez.seccloudfs.processing.impl.DistributedCloudStore}.
 *
 * @author avasquez
 */
public class DistributedCloudStoreTest {

    private static final String DATA_ID = ObjectId.get().toString();
    private static final int K = 4;
    private static final int M = 2;
    private static final int SLICE_SIZE = 10000;

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    private DistributedCloudStore cloudStore;

    @Before
    public void setUp() throws Exception {
        ErasureEncoder encoder = mock(ErasureEncoder.class);
        ErasureDecoder decoder = mock(ErasureDecoder.class);

        when(encoder.getK()).thenReturn(K);
        when(encoder.getM()).thenReturn(M);
        when(decoder.getK()).thenReturn(K);
        when(decoder.getM()).thenReturn(M);
        when(encoder.encode(any(ReadableByteChannel.class), anyInt(), any(WritableByteChannel[].class),
            any(WritableByteChannel[].class))).thenReturn(SLICE_SIZE);

        cloudStore = new DistributedCloudStore();
        cloudStore.setErasureEncoder(encoder);
        cloudStore.setErasureDecoder(decoder);
        cloudStore.setTaskExecutor(Executors.newCachedThreadPool());
        cloudStore.setTmpDir(tmpDir.getRoot().getPath());
    }

    @Test
    public void testUpload() throws Exception {
        CloudStoreRegistry registry = new CloudStoreRegistryImpl();
        registry.register(createDefaultCloudStore("store1"));
        registry.register(createDefaultCloudStore("store2"));
        registry.register(createFailingCloudStore("store3"));
        registry.register(createDefaultCloudStore("store4"));
        registry.register(createDefaultCloudStore("store5"));
        registry.register(createDefaultCloudStore("store6"));
        registry.register(createDefaultCloudStore("store7"));

        UploadRepository repository = mock(UploadRepository.class);

        cloudStore.setCloudStoreRegistry(registry);
        cloudStore.setUploadRepository(repository);

        cloudStore.upload(DATA_ID, mock(ReadableByteChannel.class), SLICE_SIZE * K);

        verify(registry.find("store1")).upload(anyString(), any(ReadableByteChannel.class), anyLong());
        verify(registry.find("store2")).upload(anyString(), any(ReadableByteChannel.class), anyLong());
        verify(registry.find("store3")).upload(anyString(), any(ReadableByteChannel.class), anyLong());
        verify(registry.find("store4")).upload(anyString(), any(ReadableByteChannel.class), anyLong());
        verify(registry.find("store5")).upload(anyString(), any(ReadableByteChannel.class), anyLong());
        verify(registry.find("store6")).upload(anyString(), any(ReadableByteChannel.class), anyLong());
        verify(registry.find("store7")).upload(anyString(), any(ReadableByteChannel.class), anyLong());
    }

    @Test
    public void testUploadWithNotEnoughStores() throws Exception {
        CloudStoreRegistry registry = new CloudStoreRegistryImpl();
        registry.register(createDefaultCloudStore("store1"));
        registry.register(createDefaultCloudStore("store2"));
        registry.register(createFailingCloudStore("store3"));
        registry.register(createDefaultCloudStore("store4"));
        registry.register(createDefaultCloudStore("store5"));
        registry.register(createDefaultCloudStore("store6"));

        UploadRepository repository = mock(UploadRepository.class);

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Upload upload = (Upload) invocation.getArguments()[0];
                upload.setId(ObjectId.get().toString());

                return null;
            }

        }).when(repository).insert(any(Upload.class));

        cloudStore.setCloudStoreRegistry(registry);
        cloudStore.setUploadRepository(repository);

        try {
            cloudStore.upload(DATA_ID, mock(ReadableByteChannel.class), SLICE_SIZE * K);
            fail("Expected " + IOException.class);
        } catch (IOException e) {
        }

        verify(registry.find("store1")).upload(anyString(), any(ReadableByteChannel.class), anyLong());
        verify(registry.find("store2")).upload(anyString(), any(ReadableByteChannel.class), anyLong());
        verify(registry.find("store3")).upload(anyString(), any(ReadableByteChannel.class), anyLong());
        verify(registry.find("store4")).upload(anyString(), any(ReadableByteChannel.class), anyLong());
        verify(registry.find("store5")).upload(anyString(), any(ReadableByteChannel.class), anyLong());
        verify(registry.find("store6")).upload(anyString(), any(ReadableByteChannel.class), anyLong());

        verify(registry.find("store1")).delete(anyString());
        verify(registry.find("store2")).delete(anyString());
        verify(registry.find("store3"), never()).delete(anyString());
        verify(registry.find("store4")).delete(anyString());
        verify(registry.find("store5")).delete(anyString());
        verify(registry.find("store6")).delete(anyString());
    }

    @Test
    public void testUploadWithAlreadyUploadedData() throws Exception {
        CloudStoreRegistry registry = new CloudStoreRegistryImpl();
        registry.register(createDefaultCloudStore("store1"));
        registry.register(createDefaultCloudStore("store2"));
        registry.register(createDefaultCloudStore("store3"));
        registry.register(createDefaultCloudStore("store4"));
        registry.register(createDefaultCloudStore("store5"));
        registry.register(createDefaultCloudStore("store6"));

        UploadRepository repository = mock(UploadRepository.class);
        Upload upload = createDefaultUpload(registry);

        when(repository.findLastSuccessfulByDataId(DATA_ID)).thenReturn(upload);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Upload upload = (Upload) invocation.getArguments()[0];
                upload.setId(ObjectId.get().toString());

                return null;
            }

        }).when(repository).insert(any(Upload.class));

        cloudStore.setCloudStoreRegistry(registry);
        cloudStore.setUploadRepository(repository);

        cloudStore.upload(DATA_ID, mock(ReadableByteChannel.class), SLICE_SIZE * K);

        verify(registry.find("store1")).upload(anyString(), any(ReadableByteChannel.class), anyLong());
        verify(registry.find("store2")).upload(anyString(), any(ReadableByteChannel.class), anyLong());
        verify(registry.find("store3")).upload(anyString(), any(ReadableByteChannel.class), anyLong());
        verify(registry.find("store4")).upload(anyString(), any(ReadableByteChannel.class), anyLong());
        verify(registry.find("store5")).upload(anyString(), any(ReadableByteChannel.class), anyLong());
        verify(registry.find("store6")).upload(anyString(), any(ReadableByteChannel.class), anyLong());

        verify(registry.find("store1")).delete(anyString());
        verify(registry.find("store2")).delete(anyString());
        verify(registry.find("store3")).delete(anyString());
        verify(registry.find("store4")).delete(anyString());
        verify(registry.find("store5")).delete(anyString());
        verify(registry.find("store6")).delete(anyString());
    }

    @Test
    public void testDownload() throws Exception {
        CloudStoreRegistry registry = new CloudStoreRegistryImpl();
        registry.register(createDefaultCloudStore("store1"));
        registry.register(createDefaultCloudStore("store2"));
        registry.register(createDefaultCloudStore("store3"));
        registry.register(createDefaultCloudStore("store4"));
        registry.register(createDefaultCloudStore("store5"));
        registry.register(createDefaultCloudStore("store6"));

        UploadRepository repository = mock(UploadRepository.class);
        Upload upload = createDefaultUpload(registry);

        when(repository.findLastSuccessfulByDataId(DATA_ID)).thenReturn(upload);

        cloudStore.setCloudStoreRegistry(registry);
        cloudStore.setUploadRepository(repository);

        cloudStore.download(DATA_ID, mock(WritableByteChannel.class));

        verify(registry.find("store1")).download(anyString(), any(WritableByteChannel.class));
        verify(registry.find("store2")).download(anyString(), any(WritableByteChannel.class));
        verify(registry.find("store3")).download(anyString(), any(WritableByteChannel.class));
        verify(registry.find("store4")).download(anyString(), any(WritableByteChannel.class));
        verify(registry.find("store5"), never()).download(anyString(), any(WritableByteChannel.class));
        verify(registry.find("store6"), never()).download(anyString(), any(WritableByteChannel.class));
    }

    @Test
    public void testDownloadWithSomeMissingSlices() throws Exception {
        CloudStoreRegistry registry = new CloudStoreRegistryImpl();
        registry.register(createDefaultCloudStore("store1"));
        registry.register(createDefaultCloudStore("store2"));
        registry.register(createFailingCloudStore("store3"));
        registry.register(createFailingCloudStore("store4"));
        registry.register(createDefaultCloudStore("store5"));
        registry.register(createDefaultCloudStore("store6"));

        UploadRepository repository = mock(UploadRepository.class);
        Upload upload = createDefaultUpload(registry);

        when(repository.findLastSuccessfulByDataId(DATA_ID)).thenReturn(upload);

        cloudStore.setCloudStoreRegistry(registry);
        cloudStore.setUploadRepository(repository);

        cloudStore.download(DATA_ID, mock(WritableByteChannel.class));

        verify(registry.find("store1")).download(anyString(), any(WritableByteChannel.class));
        verify(registry.find("store2")).download(anyString(), any(WritableByteChannel.class));
        verify(registry.find("store3")).download(anyString(), any(WritableByteChannel.class));
        verify(registry.find("store4")).download(anyString(), any(WritableByteChannel.class));
        verify(registry.find("store5")).download(anyString(), any(WritableByteChannel.class));
        verify(registry.find("store6")).download(anyString(), any(WritableByteChannel.class));
    }

    @Test
    public void testDownloadWithNotEnoughSlices() throws Exception {
        CloudStoreRegistry registry = new CloudStoreRegistryImpl();
        registry.register(createDefaultCloudStore("store1"));
        registry.register(createDefaultCloudStore("store2"));
        registry.register(createFailingCloudStore("store3"));
        registry.register(createFailingCloudStore("store4"));
        registry.register(createFailingCloudStore("store5"));
        registry.register(createDefaultCloudStore("store6"));

        UploadRepository repository = mock(UploadRepository.class);
        Upload upload = createDefaultUpload(registry);

        when(repository.findLastSuccessfulByDataId(DATA_ID)).thenReturn(upload);

        cloudStore.setCloudStoreRegistry(registry);
        cloudStore.setUploadRepository(repository);

        try {
            cloudStore.download(DATA_ID, mock(WritableByteChannel.class));
            fail("Expected " + IOException.class);
        } catch (IOException e) {
        }

        verify(registry.find("store1")).download(anyString(), any(WritableByteChannel.class));
        verify(registry.find("store2")).download(anyString(), any(WritableByteChannel.class));
        verify(registry.find("store3")).download(anyString(), any(WritableByteChannel.class));
        verify(registry.find("store4")).download(anyString(), any(WritableByteChannel.class));
        verify(registry.find("store5")).download(anyString(), any(WritableByteChannel.class));
        verify(registry.find("store6")).download(anyString(), any(WritableByteChannel.class));
    }

    @Test
    public void testDelete() throws Exception {
        CloudStoreRegistry registry = new CloudStoreRegistryImpl();
        registry.register(createDefaultCloudStore("store1"));
        registry.register(createDefaultCloudStore("store2"));
        registry.register(createFailingCloudStore("store3"));
        registry.register(createDefaultCloudStore("store4"));
        registry.register(createDefaultCloudStore("store5"));
        registry.register(createDefaultCloudStore("store6"));

        UploadRepository repository = mock(UploadRepository.class);
        Upload upload = createDefaultUpload(registry);

        when(repository.findLastSuccessfulByDataId(DATA_ID)).thenReturn(upload);

        cloudStore.setCloudStoreRegistry(registry);
        cloudStore.setUploadRepository(repository);

        cloudStore.delete(DATA_ID);

        verify(registry.find("store1")).delete(anyString());
        verify(registry.find("store2")).delete(anyString());
        verify(registry.find("store3")).delete(anyString());
        verify(registry.find("store4")).delete(anyString());
        verify(registry.find("store5")).delete(anyString());
        verify(registry.find("store6")).delete(anyString());
    }

    private CloudStore createDefaultCloudStore(final String name) {
        CloudStore store = mock(CloudStore.class);
        when(store.getName()).thenReturn(name);

        return store;
    }

    private CloudStore createFailingCloudStore(final String name) throws IOException {
        CloudStore store = mock(CloudStore.class);
        when(store.getName()).thenReturn(name);
        doThrow(IOException.class).when(store).upload(anyString(), any(ReadableByteChannel.class), anyLong());
        doThrow(IOException.class).when(store).download(anyString(), any(WritableByteChannel.class));

        return store;
    }

    private SliceMetadata[] createSliceMetadata(int num, Queue<CloudStore> stores) {
        SliceMetadata[] metadata = new SliceMetadata[num];

        for (int i = 0; i < num; i++) {
            metadata[i] = new SliceMetadata();
            metadata[i].setId(SliceMetadata.generateId());
            metadata[i].setCloudStoreName(stores.poll().getName());
        }

        return metadata;
    }

    private Upload createDefaultUpload(CloudStoreRegistry registry) {
        Queue<CloudStore> stores = new LinkedList<>(registry.list());

        Upload upload = new Upload();
        upload.setId(ObjectId.get().toString());
        upload.setDataId(DATA_ID);
        upload.setDataSize(SLICE_SIZE * K);
        upload.setFinishDate(new Date());
        upload.setSuccess(true);
        upload.setDataSliceMetadata(createSliceMetadata(K, stores));
        upload.setCodingSliceMetadata(createSliceMetadata(M, stores));

        return upload;
    }

}
