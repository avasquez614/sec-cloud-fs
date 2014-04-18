package org.avasquez.seccloudfs.erasure;

import java.nio.ByteBuffer;

/**
 * Erasure coding encoder.
 *
 * @author avasquez
 */
public interface ErasureEncoder {

    /**
     * Encodes the given raw data through an erasure coding algorithm, producing k + m fragments.
     *
     * @param rawData the data to encode
     *
     * @return  the result with the encoded fragments
     */
    EncodingResult encode(ByteBuffer rawData);

}
