package org.avasquez.seccloudfs.processing.utils.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.processing.db.model.EncryptionKey;
import org.avasquez.seccloudfs.processing.db.repos.EncryptionKeyRepository;
import org.avasquez.seccloudfs.utils.CryptoUtils;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.io.CipherInputStream;
import org.bouncycastle.crypto.io.CipherOutputStream;
import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.core.io.ClassPathResource;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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
    private byte[] iv;
    private EncryptionKeyRepository keyRepository;
    private EncryptingCloudStore cloudStore;
    private CloudStore underlyingStore;
    private File encryptedFile;

    @Before
    public void setUp() throws Exception {
        keyRepository = mock(EncryptionKeyRepository.class);
        when(keyRepository.findByDataId(DATA_ID)).thenAnswer(new Answer<EncryptionKey>() {

            @Override
            public EncryptionKey answer(final InvocationOnMock invocation) throws Throwable {
                return new EncryptionKey(DATA_ID, key, iv);
            }

        });
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                key = ((EncryptionKey)invocation.getArguments()[0]).getKey();
                iv = ((EncryptionKey)invocation.getArguments()[0]).getIv();

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

        decrypt(new FileInputStream(encryptedFile), new FileOutputStream(decryptedFile), key, iv);

        assertArrayEquals(FileUtils.readFileToByteArray(rawFile), FileUtils.readFileToByteArray(decryptedFile));
    }

    @Test
    public void testDownload() throws Exception {
        File rawFile = new ClassPathResource("gpl-3.0.txt").getFile();
        encryptedFile = tmpDir.newFile();
        key = CryptoUtils.generateRandomBytes(16);
        iv = CryptoUtils.generateRandomBytes(16);

        encrypt(new FileInputStream(rawFile), new FileOutputStream(encryptedFile), key, iv);

        File decryptedFile = tmpDir.newFile();
        Path decryptedFilePath = decryptedFile.toPath();

        try (FileChannel channel = FileChannel.open(decryptedFilePath, StandardOpenOption.WRITE)) {
            cloudStore.download(DATA_ID, channel);
        }

        assertArrayEquals(FileUtils.readFileToByteArray(rawFile), FileUtils.readFileToByteArray(decryptedFile));
    }

    @Test
    public void testDownloadWithModifiedData() throws Exception {
        File rawFile = new ClassPathResource("gpl-3.0.txt").getFile();
        encryptedFile = tmpDir.newFile();
        key = CryptoUtils.generateRandomBytes(16);
        iv = CryptoUtils.generateRandomBytes(16);

        encrypt(new FileInputStream(rawFile), new FileOutputStream(encryptedFile), key, iv);

        try (FileChannel channel = FileChannel.open(encryptedFile.toPath(), StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.putInt(1986);
            buffer.rewind();

            channel.write(buffer, 1000);
        }

        File decryptedFile = tmpDir.newFile();
        Path decryptedFilePath = decryptedFile.toPath();

        try (FileChannel channel = FileChannel.open(decryptedFilePath, StandardOpenOption.WRITE)) {
            cloudStore.download(DATA_ID, channel);
            fail("Expected exception");
        } catch (Exception e) {
            Throwable cause = ExceptionUtils.getRootCause(e);

            assertNotNull(cause);
            assertTrue(cause.getClass().equals(InvalidCipherTextException.class));
            assertEquals("mac check in GCM failed", cause.getMessage());
        }
    }

    private void encrypt(InputStream in, OutputStream out, byte[] key, byte[] iv) throws IOException {
        try (CipherOutputStream cipherOut = new CipherOutputStream(out, createEncryptionCipher(key, iv))) {
            IOUtils.copy(in, cipherOut);
        }
    }

    private void decrypt(InputStream in, OutputStream out, byte[] key, byte[] iv) throws IOException {
        try (CipherInputStream cipherIn = new CipherInputStream(in, createDecryptionCipher(key, iv))) {
            IOUtils.copy(cipherIn, out);
        }
    }

    private AEADBlockCipher createEncryptionCipher(byte[] key, byte[] iv) {
        return CryptoUtils.createAesWithGcmCipher(true, key, iv);
    }

    private AEADBlockCipher createDecryptionCipher(byte[] key, byte[] iv) {
        return CryptoUtils.createAesWithGcmCipher(false, key, iv);
    }

}
