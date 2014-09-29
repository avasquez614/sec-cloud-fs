/*
 * Copyright (C) 2007-2014 Crafter Software Corporation.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.avasquez.seccloudfs.utils;

import java.security.SecureRandom;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 * Utility methods for crypto operations.
 *
 * @author avasquez
 */
public class CryptoUtils {

    public static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a random array of bytes, using the singleton {@link java.security.SecureRandom}.
     *
     * @param size the size of the array
     * @return the generated array
     */
    public static byte[] generateRandomBytes(int size) {
        byte[] bytes = new byte[size];

        secureRandom.nextBytes(bytes);

        return bytes;
    }

    /**
     * Creates a cipher that uses AES encryption with GCM block mode.
     *
     * @param forEncryption true if the cipher is going to be used for encryption, false for decryption
     * @param key           the encryption key
     * @param iv            the initialization vector, or nonce
     *
     * @return the initialized cipher
     */
    public static AEADBlockCipher createAesWithGcmCipher(boolean forEncryption, byte[] key, byte[] iv) {
        AEADBlockCipher cipher = new GCMBlockCipher(new AESEngine());
        cipher.init(forEncryption, new AEADParameters(new KeyParameter(key), 128, iv));

        return cipher;
    }

}
