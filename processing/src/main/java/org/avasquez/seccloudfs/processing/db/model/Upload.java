package org.avasquez.seccloudfs.processing.db.model;

import org.jongo.marshall.jackson.oid.Id;
import org.jongo.marshall.jackson.oid.ObjectId;

import java.util.Date;

/**
 * Represents single upload operation.
 *
 * @author avasquez
 */
public class Upload {

    @Id
    @ObjectId
    private String id;
    private String dataId;
    private int dataSize;
    private Date finishDate;
    private boolean success;
    private SliceMetadata[] dataSliceMetadata;
    private SliceMetadata[] codingSliceMetadata;

    public Upload() {
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
    public void setId(String id) {
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
    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    /**
     * Returns the original data size
     */
    public int getDataSize() {
        return dataSize;
    }

    /**
     * Sets the original data size.
     */
    public void setDataSize(int dataSize) {
        this.dataSize = dataSize;
    }

    /**
     * Returns the date the upload finished
     */
    public Date getFinishDate() {
        return finishDate;
    }

    /**
     * Sets the date the upload was finished.
     */
    public void setFinishDate(Date finishDate) {
        this.finishDate = finishDate;
    }

    /**
     * Returns true if the upload was a success.
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Sets true if the upload ws a success.
     */
    public void setSuccess(boolean success) {
        this.success = success;
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
    public void setDataSliceMetadata(SliceMetadata[] dataSliceMetadata) {
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
    public void setCodingSliceMetadata(SliceMetadata[] codingSliceMetadata) {
        this.codingSliceMetadata = codingSliceMetadata;
    }

}
