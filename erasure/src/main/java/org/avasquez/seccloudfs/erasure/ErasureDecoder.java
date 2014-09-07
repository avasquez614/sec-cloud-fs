package org.avasquez.seccloudfs.erasure;

import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Erasure coding decoder.
 *
 * @author avasquez
 */
public interface ErasureDecoder {

    /**
     * Returns the number of data slices that are expected when decoding.
     */
    int getK();

    /**
     * Returns the number of coding slices that are expected when decoding.
     */
    int getM();

    /**
     * Decodes the given data and coding slices through an erasure coding algorithm, producing the original data.
     *
     * @param originalSize  size of the original data
     * @param dataSlices    the data (k) slices. Missing, or "erased", slices should be indicated with {@code null}.
     * @param codingSlices  the coding (m) slices. Missing, or "erased", slices should be indicated with {@code null}.
     * @param output        an output channel to write the original data to
     */
    void decode(int originalSize, ReadableByteChannel[] dataSlices, ReadableByteChannel[] codingSlices,
                WritableByteChannel output) throws DecodingException;

}
