package org.avasquez.seccloudfs.processing.utils.crypto;

import org.apache.commons.io.IOUtils;
import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.processing.db.model.EncryptionKey;
import org.avasquez.seccloudfs.processing.db.repos.EncryptionKeyRepository;
import org.avasquez.seccloudfs.utils.CryptoUtils;
import org.bouncycastle.crypto.io.CipherInputStream;
import org.bouncycastle.crypto.io.CipherOutputStream;
import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * {@link org.avasquez.seccloudfs.cloud.CloudStore} decorator that encrypts the data before upload and decrypts it
 * after download, using AES with GCM cipher mode.
 *
 * @author avasquez
 */
public class EncryptingCloudStore implements CloudStore {

    private static final Logger logger = LoggerFactory.getLogger(EncryptingCloudStore.class);

    private static final int AES_KEY_BYTE_SIZE = 16;

    private CloudStore underlyingStore;
    private EncryptionKeyRepository keyRepository;
    private Path tmpDir;

    @Required
    public void setUnderlyingStore(CloudStore underlyingStore) {
        this.underlyingStore = underlyingStore;
    }

    @Required
    public void setKeyRepository(EncryptionKeyRepository keyRepository) {
        this.keyRepository = keyRepository;
    }

    @Required
    public void setTmpDir(String tmpDirPath) {
        tmpDir = Paths.get(tmpDirPath);
    }

    @Override
    public String getName() {
        return underlyingStore.getName();
    }

    @Override
    public void upload(String id, ReadableByteChannel src, long length) throws IOException {
        Path tmpFile = Files.createTempFile(tmpDir, id, ".encrypted");

        try {
            InputStream in = Channels.newInputStream(src);
            OutputStream out = new FileOutputStream(tmpFile.toFile());
            byte[] key = CryptoUtils.generateRandomBytes(AES_KEY_BYTE_SIZE);
            byte[] iv = CryptoUtils.generateRandomBytes(AES_KEY_BYTE_SIZE);

            encrypt(id, in, out, key, iv);
            saveEncryptionKey(id, key, iv);

            logger.debug("Data '{}' successfully encrypted", id);
        } catch (Exception e) {
            Files.delete(tmpFile);

            throw e;
        }

        try (FileChannel tmpChannel = FileChannel.open(tmpFile, StandardOpenOption.READ,
             StandardOpenOption.DELETE_ON_CLOSE)) {
            underlyingStore.upload(id, tmpChannel, tmpChannel.size());
        }
    }

    @Override
    public void download(String id, WritableByteChannel target) throws IOException {
        Path tmpFile = Files.createTempFile(tmpDir, id, ".decrypted");

        try (FileChannel tmpChannel = FileChannel.open(tmpFile, StandardOpenOption.WRITE,
             StandardOpenOption.TRUNCATE_EXISTING)) {
            underlyingStore.download(id, tmpChannel);
        }

        try {
            InputStream in = new FileInputStream(tmpFile.toFile());
            OutputStream out = Channels.newOutputStream(target);
            EncryptionKey key = getEncryptionKey(id);

            decrypt(id, in, out, key.getKey(), key.getIv());

            logger.debug("Data '{}' successfully decrypted", id);
        } finally {
            Files.delete(tmpFile);
        }
    }

    @Override
    public void delete(String id) throws IOException {
        deleteEncryptionKey(id);

        underlyingStore.delete(id);
    }

    private void encrypt(String dataId, InputStream in, OutputStream out, byte[] key, byte[] iv) throws IOException {
        try (CipherOutputStream cipherOut = new CipherOutputStream(out, createEncryptionCipher(key, iv))) {
            IOUtils.copy(in, cipherOut);
        } catch (Exception e) {
            throw new IOException("Failed to encrypt data '" + dataId + "'", e);
        }
    }

    private void decrypt(String dataId, InputStream in, OutputStream out, byte[] key, byte[] iv) throws IOException {
        try (CipherInputStream cipherIn = new CipherInputStream(in, createDecryptionCipher(key, iv))) {
            IOUtils.copy(cipherIn, out);
        } catch (Exception e) {
            throw new IOException("Failed to decrypt data '" + dataId + "'", e);
        }
    }

    private EncryptionKey getEncryptionKey(String dataId) throws IOException {
        try {
            EncryptionKey key = keyRepository.findByDataId(dataId);
            if (key != null) {
                return key;
            } else {
                throw new IOException("No encryption key found for data ID '" + dataId + "'");
            }
        } catch (DbException e) {
            throw new IOException("Unable to retrieve encryption key for data '" + dataId + "' in repository", e);
        }
    }

    private void saveEncryptionKey(String dataId, byte[] key, byte[] iv) throws IOException {
        try {
            keyRepository.insert(new EncryptionKey(dataId, key, iv));
        } catch (DbException e) {
            throw new IOException("Unable to save encryption key for data '" + dataId + "' in repository", e);
        }
    }

    private void deleteEncryptionKey(String dataId) throws IOException {
        try {
            keyRepository.deleteByDataId(dataId);
        } catch (DbException e) {
            throw new IOException("Unable to delete encryption key for data '" + dataId + "' in repository", e);
        }
    }

    private AEADBlockCipher createEncryptionCipher(byte[] key, byte[] iv) {
        return CryptoUtils.createAesWithGcmCipher(true, key, iv);
    }

    private AEADBlockCipher createDecryptionCipher(byte[] key, byte[] iv) {
        return CryptoUtils.createAesWithGcmCipher(false, key, iv);
    }

}
