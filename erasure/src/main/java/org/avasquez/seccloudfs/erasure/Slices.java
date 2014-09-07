package org.avasquez.seccloudfs.erasure;

import java.nio.channels.ReadableByteChannel;

/**
 * Contains the results of erasure encoding: k data slices and m coding slices.
 *
 * @author avasquez
 */
public class Slices {

    private ReadableByteChannel[] dataSlices;
    private ReadableByteChannel[] codingSlices;
    private int sliceSize;

    public Slices(ReadableByteChannel[] dataSlices, ReadableByteChannel[] codingSlices, int sliceSize) {
        this.dataSlices = dataSlices;
        this.codingSlices = codingSlices;
        this.sliceSize = sliceSize;
    }

    /**
     * Returns an array of k data slices.
     */
    public ReadableByteChannel[] getDataSlices() {
        return dataSlices;
    }

    /**
     * Returns an array of m coding slices.
     */
    public ReadableByteChannel[] getCodingSlices() {
        return codingSlices;
    }

    /**
     * Returns the size of each slice.
     */
    public int getSliceSize() {
        return sliceSize;
    }

}
