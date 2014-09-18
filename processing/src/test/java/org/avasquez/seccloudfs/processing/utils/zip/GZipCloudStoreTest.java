package org.avasquez.seccloudfs.processing.utils.zip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.avasquez.seccloudfs.cloud.CloudStore;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.core.io.ClassPathResource;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link org.avasquez.seccloudfs.processing.utils.zip.GZipCloudStore}.
 *
 * @author avasquez
 */
public class GZipCloudStoreTest {

    private static final String DATA_ID = ObjectId.get().toString();

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    private GZipCloudStore cloudStore;
    private CloudStore underlyingStore;
    private File zippedFile;

    @Before
    public void setUp() throws Exception {
        underlyingStore = mock(CloudStore.class);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                zippedFile = tmpDir.newFile();
                ReadableByteChannel src = (ReadableByteChannel)invocation.getArguments()[1];

                IOUtils.copyLarge(Channels.newInputStream(src), new FileOutputStream(zippedFile));

                return null;
            }

        }).when(underlyingStore).upload(anyString(), any(ReadableByteChannel.class), anyLong());
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                WritableByteChannel target = (WritableByteChannel)invocation.getArguments()[1];

                IOUtils.copyLarge(new FileInputStream(zippedFile), Channels.newOutputStream(target));

                return null;
            }

        }).when(underlyingStore).download(anyString(), any(WritableByteChannel.class));

        cloudStore = new GZipCloudStore();
        cloudStore.setUnderlyingStore(underlyingStore);
        cloudStore.setTmpDir(tmpDir.getRoot().getAbsolutePath());
    }

    @Test
    public void testUpload() throws Exception {
        File rawFile = new ClassPathResource("gpl-3.0.txt").getFile();
        Path rawFilePath = rawFile.toPath();

        try (FileChannel channel = FileChannel.open(rawFilePath, StandardOpenOption.READ)) {
            cloudStore.upload(DATA_ID, channel, channel.size());
        }

        File unzippedFile = tmpDir.newFile();

        IOUtils.copy(new GZIPInputStream(new FileInputStream(zippedFile)), new FileOutputStream(unzippedFile));

        assertArrayEquals(FileUtils.readFileToByteArray(rawFile), FileUtils.readFileToByteArray(unzippedFile));
    }

    @Test
    public void testDownload() throws Exception {
        File rawFile = new ClassPathResource("gpl-3.0.txt").getFile();
        zippedFile = tmpDir.newFile();

        InputStream rawFileInputStream = new FileInputStream(rawFile);
        GZIPOutputStream zipOutputStream = new GZIPOutputStream(new FileOutputStream(zippedFile));

        IOUtils.copyLarge(rawFileInputStream, zipOutputStream);

        zipOutputStream.finish();

        File unzippedFile = tmpDir.newFile();
        Path unzippedFilePath = unzippedFile.toPath();

        try (FileChannel channel = FileChannel.open(unzippedFilePath, StandardOpenOption.WRITE)) {
            cloudStore.download(DATA_ID, channel);
        }

        assertArrayEquals(FileUtils.readFileToByteArray(rawFile), FileUtils.readFileToByteArray(unzippedFile));
    }

}
