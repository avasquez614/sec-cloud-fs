package org.avasquez.seccloudfs.storage.db.model;

import java.util.List;

import org.jongo.marshall.jackson.oid.Id;
import org.jongo.marshall.jackson.oid.ObjectId;

/**
 * Information about an erasure encoding operation.
 *
 * @author avasquez
 */
public class ErasureInfo {

    @Id
    @ObjectId
    private String id;
    private String dataId;
    private int sliceSize;
    private List<SliceMetadata> dataSliceMetadata;
    private List<SliceMetadata> codingSliceMetadata;

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
    public List<SliceMetadata> getDataSliceMetadata() {
        return dataSliceMetadata;
    }

    /**
     * Sets the metadata for the data slices.
     */
    public void setDataSliceMetadata(final List<SliceMetadata> dataSliceMetadata) {
        this.dataSliceMetadata = dataSliceMetadata;
    }

    /**
     * Returns the metadata for the coding slices.
     */
    public List<SliceMetadata> getCodingSliceMetadata() {
        return codingSliceMetadata;
    }

    /**
     * Sets the metadata for the data slices.
     */
    public void setCodingSliceMetadata(final List<SliceMetadata> codingSliceMetadata) {
        this.codingSliceMetadata = codingSliceMetadata;
    }

}
