package org.avasquez.seccloudfs.processing.impl;

import java.nio.channels.FileChannel;

/**
 * Result of a {@link DownloadTask}.
 *
 * @author avasquez
 */
public class DownloadResult {

    private FileChannel slice;
    private boolean dataSlice;
    private int sliceIndex;

    public DownloadResult(FileChannel slice, boolean dataSlice, int sliceIndex) {
        this.slice = slice;
        this.dataSlice = dataSlice;
        this.sliceIndex = sliceIndex;
    }

    /**
     * Returns the slice data.
     */
    public FileChannel getSlice() {
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
