package org.avasquez.seccloudfs.erasure;

import java.nio.ByteBuffer;

/**
 * Erasure coding decoder.
 *
 * @author avasquez
 */
public interface ErasureDecoder {

    /**
     * Decodes the given data and coding fragments through an erasure coding algorithm, producing the original data.
     *
     * @param dataFragments     the array of data (k) fragments. Size should be the same size of fragments returned
     *                          by {@link org.avasquez.seccloudfs.erasure.ErasureEncoder#encode(java.nio.ByteBuffer)}.
     *                          Missing, or "erased", fragments should be indicated with {@code null}.
     * @param encodedFragments  the array of coding (m) fragments. Size should be the same size of fragments returned
     *                          by {@link org.avasquez.seccloudfs.erasure.ErasureEncoder#encode(java.nio.ByteBuffer)}.
     *                          Missing, or "erased", fragments should be indicated with {@code null}.
     *
     * @return the original data
     */
    ByteBuffer decode(ByteBuffer[] dataFragments, ByteBuffer[] encodedFragments);

}
