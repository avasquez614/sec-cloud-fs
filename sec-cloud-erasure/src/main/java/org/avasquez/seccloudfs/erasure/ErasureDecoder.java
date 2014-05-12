package org.avasquez.seccloudfs.erasure;

import java.nio.channels.WritableByteChannel;

/**
 * Erasure coding decoder.
 *
 * @author avasquez
 */
public interface ErasureDecoder {

    /**
     * Decodes the given data and coding fragments through an erasure coding algorithm, producing the original data.
     *
     * @param fragments     the data (k) and coding (m) fragments. Missing, or "erased", fragments should be indicated
     *                      with {@code null}.
     * @param originalSize  size of the original data
     * @param outputChannel an output channel to write the original data to
     */
    void decode(Fragments fragments, int originalSize, WritableByteChannel outputChannel) throws DecodingException;

}
