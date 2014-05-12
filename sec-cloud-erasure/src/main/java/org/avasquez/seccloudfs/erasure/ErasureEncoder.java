package org.avasquez.seccloudfs.erasure;

import java.nio.channels.ReadableByteChannel;

/**
 * Erasure coding encoder.
 *
 * @author avasquez
 */
public interface ErasureEncoder {

    /**
     * Encodes the given raw data through an erasure coding algorithm, producing k + m fragments.
     *
     * @param inputChannel      an input channel to read the raw data from
     * @param size              size of the raw data
     *
     * @return  the data (k) and coding (m) fragments
     */
    Fragments encode(ReadableByteChannel inputChannel, int size) throws EncodingException;

}
