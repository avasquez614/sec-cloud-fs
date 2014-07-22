package org.avasquez.seccloudfs.erasure;

import java.nio.channels.WritableByteChannel;

/**
 * Erasure coding decoder.
 *
 * @author avasquez
 */
public interface ErasureDecoder {

    /**
     * Decodes the given data and coding slices through an erasure coding algorithm, producing the original data.
     *
     * @param slices        the data (k) and coding (m) slices. Missing, or "erased", slices should be indicated
     *                      with {@code null}.
     * @param originalSize  size of the original data
     * @param outputChannel an output channel to write the original data to
     */
    void decode(Slices slices, int originalSize, WritableByteChannel outputChannel) throws DecodingException;

}
