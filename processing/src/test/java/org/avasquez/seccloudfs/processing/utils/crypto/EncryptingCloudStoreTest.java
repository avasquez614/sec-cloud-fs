package org.avasquez.seccloudfs.processing.utils.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.crypto.AbstractSymmetricCipherService;
import org.apache.shiro.crypto.AesCipherService;
import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.processing.db.model.EncryptionKey;
import org.avasquez.seccloudfs.processing.db.repos.EncryptionKeyRepository;
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
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.avasquez.seccloudfs.processing.utils.crypto.EncryptingCloudStore}.
 *
 * @author avasquez
 */
public class EncryptingCloudStoreTest {

    private static final String DATA_ID = ObjectId.get().toString();

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    private byte[] key;
    private AbstractSymmetricCipherService cipherService;
    private EncryptionKeyRepository keyRepository;
    private EncryptingCloudStore cloudStore;
    private CloudStore underlyingStore;
    private File encryptedFile;

    @Before
    public void setUp() throws Exception {
        cipherService = new AesCipherService();

        keyRepository = mock(EncryptionKeyRepository.class);
        when(keyRepository.findByDataId(DATA_ID)).thenAnswer(new Answer<EncryptionKey>() {

            @Override
            public EncryptionKey answer(final InvocationOnMock invocation) throws Throwable {
                return new EncryptionKey(DATA_ID, key);
            }

        });
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                key = ((EncryptionKey)invocation.getArguments()[0]).getKey();

                return null;
            }

        }).when(keyRepository).insert(any(EncryptionKey.class));

        underlyingStore = mock(CloudStore.class);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                encryptedFile = tmpDir.newFile();
                ReadableByteChannel src = (ReadableByteChannel)invocation.getArguments()[1];

                IOUtils.copyLarge(Channels.newInputStream(src), new FileOutputStream(encryptedFile));

                return null;
            }

        }).when(underlyingStore).upload(anyString(), any(ReadableByteChannel.class), anyLong());
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                WritableByteChannel target = (WritableByteChannel)invocation.getArguments()[1];

                IOUtils.copyLarge(new FileInputStream(encryptedFile), Channels.newOutputStream(target));

                return null;
            }

        }).when(underlyingStore).download(anyString(), any(WritableByteChannel.class));

        cloudStore = new EncryptingCloudStore();
        cloudStore.setCipherService(cipherService);
        cloudStore.setKeyRepository(keyRepository);
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

        File decryptedFile = tmpDir.newFile();

        cipherService.decrypt(new FileInputStream(encryptedFile), new FileOutputStream(decryptedFile), key);

        assertArrayEquals(FileUtils.readFileToByteArray(rawFile), FileUtils.readFileToByteArray(decryptedFile));
    }

    @Test
    public void testDownload() throws Exception {
        File rawFile = new ClassPathResource("gpl-3.0.txt").getFile();
        encryptedFile = tmpDir.newFile();
        key = cipherService.generateNewKey().getEncoded();

        cipherService.encrypt(new FileInputStream(rawFile), new FileOutputStream(encryptedFile), key);

        File decryptedFile = tmpDir.newFile();
        Path decryptedFilePath = decryptedFile.toPath();

        try (FileChannel channel = FileChannel.open(decryptedFilePath, StandardOpenOption.WRITE)) {
            cloudStore.download(DATA_ID, channel);
        }

        assertArrayEquals(FileUtils.readFileToByteArray(rawFile), FileUtils.readFileToByteArray(decryptedFile));
    }

}
