package org.avasquez.seccloudfs.erasure;

import java.nio.ByteBuffer;

/**
 * Contains the results of erasure encoding: k data blocks and m encoded fragments.
 *
 * @author avasquez
 */
public class EncodingResult {

    private ByteBuffer[] dataFragments;
    private ByteBuffer[] encodedFragments;

    public EncodingResult(ByteBuffer[] dataFragments, ByteBuffer[] encodedFragments) {
        this.dataFragments = dataFragments;
        this.encodedFragments = encodedFragments;
    }

    /**
     * Returns an array of k data fragments.
     */
    public ByteBuffer[] getDataFragments() {
        return dataFragments;
    }

    /**
     * Returns an array of m encoded fragments.
     */
    public ByteBuffer[] getEncodedFragments() {
        return encodedFragments;
    }

}
