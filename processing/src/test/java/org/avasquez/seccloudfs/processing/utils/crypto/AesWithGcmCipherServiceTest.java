package org.avasquez.seccloudfs.processing.utils.crypto;

import org.apache.commons.io.IOUtils;
import org.apache.shiro.crypto.CryptoException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import javax.crypto.AEADBadTagException;
import java.io.InputStream;
import java.security.Key;
import java.security.Security;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link org.avasquez.seccloudfs.processing.utils.crypto.AesWithGcmCipherService}.
 *
 * @author avasquez
 */
public class AesWithGcmCipherServiceTest {

    private AesWithGcmCipherService cipherService;

    @Before
    public void setUp() {
        Security.addProvider(new BouncyCastleProvider());

        cipherService = new AesWithGcmCipherService();
    }

    @Test
    public void testCipher() throws Exception {
        Key key = cipherService.generateNewKey();
        InputStream rawData = new ClassPathResource("gpl-3.0.txt").getInputStream();
        byte[] rawBytes = IOUtils.toByteArray(rawData);

        byte[] encryptedBytes = cipherService.encrypt(rawBytes, key.getEncoded()).getBytes();
        byte[] decryptedBytes = cipherService.decrypt(encryptedBytes, key.getEncoded()).getBytes();

        assertArrayEquals(rawBytes, decryptedBytes);

        encryptedBytes[100] = 25;

        try {
            cipherService.decrypt(encryptedBytes, key.getEncoded()).getBytes();
            fail("Decryption expected to fail");
        } catch (CryptoException e) {
            Throwable cause = e.getCause();

            assertNotNull(cause);
            assertTrue(cause.getClass().equals(AEADBadTagException.class));
            assertEquals("mac check in GCM failed", cause.getMessage());
        }
    }

}
