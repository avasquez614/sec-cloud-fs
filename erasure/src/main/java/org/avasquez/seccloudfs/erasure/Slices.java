package org.avasquez.seccloudfs.erasure;

import java.nio.ByteBuffer;

/**
 * Contains the results of erasure encoding: k data slices and m coding slices.
 *
 * @author avasquez
 */
public class Slices {

    private ByteBuffer[] dataSlices;
    private ByteBuffer[] codingSlices;

    public Slices(ByteBuffer[] dataSlices, ByteBuffer[] codingSlices) {
        this.dataSlices = dataSlices;
        this.codingSlices = codingSlices;
    }

    /**
     * Returns an array of k data slices.
     */
    public ByteBuffer[] getDataSlices() {
        return dataSlices;
    }

    /**
     * Returns an array of m coding slices.
     */
    public ByteBuffer[] getCodingSlices() {
        return codingSlices;
    }

}
