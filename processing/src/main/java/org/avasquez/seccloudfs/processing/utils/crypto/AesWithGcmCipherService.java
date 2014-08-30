package org.avasquez.seccloudfs.processing.utils.crypto;

import org.apache.shiro.crypto.AesCipherService;
import org.apache.shiro.crypto.OperationMode;
import org.apache.shiro.crypto.PaddingScheme;

/**
 * Extension of {@link org.apache.shiro.crypto.AesCipherService} that uses GCM (Galois/Counter Mode) as the default
 * block cipher mode.
 *
 * @author avasquez
 */
public class AesWithGcmCipherService extends AesCipherService {

    /**
     * Creates a new {@link org.apache.shiro.crypto.CipherService} instance using the {@code AES} cipher algorithm
     * with the following important cipher default attributes:
     * <table>
     * <tr>
     * <th>Attribute</th>
     * <th>Value</th>
     * </tr>
     * <tr>
     * <td>{@link #setKeySize keySize}</td>
     * <td>{@code 128} bits</td>
     * </tr>
     * <tr>
     * <td>{@link #setBlockSize blockSize}</td>
     * <td>{@code 128} bits (required for {@code AES}</td>
     * </tr>
     * <tr>
     * <td>{@link #setMode mode}</td>
     * <td>{@link org.apache.shiro.crypto.OperationMode#GCM GCM}</td>
     * </tr>
     * <tr>
     * <td>{@link #setPaddingScheme paddingScheme}</td>
     * <td>{@link org.apache.shiro.crypto.PaddingScheme#NONE NONE}</td>
     * </tr>
     * <tr>
     * <td>{@link #setInitializationVectorSize(int) initializationVectorSize}</td>
     * <td>{@code 128} bits</td>
     * </tr>
     * <tr>
     * <td>{@link #setGenerateInitializationVectors(boolean) generateInitializationVectors}</td>
     * <td>{@code true}<b>*</b></td>
     * </tr>
     * </table>
     */
    public AesWithGcmCipherService() {
        setMode(OperationMode.GCM);
        setPaddingScheme(PaddingScheme.NONE);
    }

}
