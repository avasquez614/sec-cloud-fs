package org.avasquez.seccloudfs.storage.db.model;

import org.jongo.marshall.jackson.oid.Id;
import org.jongo.marshall.jackson.oid.ObjectId;

/**
 * Contains erasure encoding info for a specific data.
 *
 * @author avasquez
 */
public class ErasureInfo {

    @Id
    @ObjectId
    private String id;
    private String dataId;
    private int sliceSize;
    private SliceMetadata[] dataSliceMetadata;
    private SliceMetadata[] codingSliceMetadata;

    public ErasureInfo() {
    }

    /**
     * Copy constructor
     *
     * @param info the info to copy
     */
    public ErasureInfo(ErasureInfo info) {
        id = info.id;
        dataId = info.dataId;
        sliceSize = info.sliceSize;
        dataSliceMetadata = info.dataSliceMetadata;
        codingSliceMetadata = info.codingSliceMetadata;
    }

    /**
     * Returns the ID of this erasure encoding operation.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the ID of this erasure encoding operation.
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * Returns the ID of the data that was encoded.
     */
    public String getDataId() {
        return dataId;
    }

    /**
     * Sets the ID of the data that was encoded.
     */
    public void setDataId(final String dataId) {
        this.dataId = dataId;
    }

    /**
     * Returns the size of all slices.
     */
    public int getSliceSize() {
        return sliceSize;
    }

    /**
     * Sets the size of all slices.
     */
    public void setSliceSize(final int sliceSize) {
        this.sliceSize = sliceSize;
    }

    /**
     * Returns the metadata for the data slices.
     */
    public SliceMetadata[] getDataSliceMetadata() {
        return dataSliceMetadata;
    }

    /**
     * Sets the metadata for the data slices.
     */
    public void setDataSliceMetadata(final SliceMetadata[] dataSliceMetadata) {
        this.dataSliceMetadata = dataSliceMetadata;
    }

    /**
     * Returns the metadata for the coding slices.
     */
    public SliceMetadata[] getCodingSliceMetadata() {
        return codingSliceMetadata;
    }

    /**
     * Sets the metadata for the data slices.
     */
    public void setCodingSliceMetadata(final SliceMetadata[] codingSliceMetadata) {
        this.codingSliceMetadata = codingSliceMetadata;
    }

}
