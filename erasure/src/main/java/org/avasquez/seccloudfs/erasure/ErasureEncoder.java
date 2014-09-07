package org.avasquez.seccloudfs.erasure;

import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Erasure coding encoder.
 *
 * @author avasquez
 */
public interface ErasureEncoder {

    /**
     * Returns the number of data slices that will be generated.
     */
    int getK();

    /**
     * Returns the number of coding slices that will be generated.
     */
    int getM();

    /**
     * Encodes the given raw data through an erasure coding algorithm, producing k + m slicess.
     *
     * @param input         an input channel to read the raw data from
     * @param size          size of the raw data
     * @param dataSlices    the channels where to write the data slices to (must be an array of k length)
     * @param codingSlices  the channels where to write the coding slices to (must be an array of m length)
     *
     * @return size of each slice
     */
    int encode(ReadableByteChannel input, int size, WritableByteChannel[] dataSlices,
               WritableByteChannel[] codingSlices) throws EncodingException;

}
