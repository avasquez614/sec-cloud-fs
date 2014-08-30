package org.avasquez.seccloudfs.processing.impl;

import java.nio.ByteBuffer;

/**
 * Result of a {@link DownloadTask}.
 *
 * @author avasquez
 */
public class DownloadResult {

    private ByteBuffer slice;
    private boolean dataSlice;
    private int sliceIndex;

    public DownloadResult(final ByteBuffer slice, final boolean dataSlice, final int sliceIndex) {
        this.slice = slice;
        this.dataSlice = dataSlice;
        this.sliceIndex = sliceIndex;
    }

    /**
     * Returns the slice data.
     */
    public ByteBuffer getSlice() {
        return slice;
    }

    /**
     * Returns true if it's a data slice, false if it's a coding slice.
     */
    public boolean isDataSlice() {
        return dataSlice;
    }

    /**
     * Returns the data or coding slice index.
     */
    public int getSliceIndex() {
        return sliceIndex;
    }

}
