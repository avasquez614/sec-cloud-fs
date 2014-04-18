package org.avasquez.seccloudfs.erasure;

import java.nio.ByteBuffer;

/**
 * Contains the results of erasure encoding: k data fragments and m coding fragments.
 *
 * @author avasquez
 */
public class EncodingResult {

    private ByteBuffer[] dataFragments;
    private ByteBuffer[] codingFragments;

    public EncodingResult(ByteBuffer[] dataFragments, ByteBuffer[] codingFragments) {
        this.dataFragments = dataFragments;
        this.codingFragments = codingFragments;
    }

    /**
     * Returns an array of k data fragments.
     */
    public ByteBuffer[] getDataFragments() {
        return dataFragments;
    }

    /**
     * Returns an array of m data fragments.
     */
    public ByteBuffer[] getCodingFragments() {
        return codingFragments;
    }

}
