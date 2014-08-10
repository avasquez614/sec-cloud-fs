package org.avasquez.seccloudfs.storage.db.model;

import org.bson.types.ObjectId;
import org.jongo.marshall.jackson.oid.Id;

/**
 * The slice metadata, stored in a {@link org.avasquez.seccloudfs.cloud.CloudStore} to be used for later fragment
 * retrieval.
 * <p>
 * It's important to point out that for slices the ID is generated in the server side because the slice content
 * is uploaded before the metadata is saved in the DB, and the ID is used also by the store.
 * </p>
 *
 * @author avasquez
 */
public class SliceMetadata {

    public enum SliceType {
        DATA("d"),
        CODING("c");

        private String prefix;

        private SliceType(final String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return prefix;
        }
    }

    @Id
    private String id;
    private String dataId;
    private SliceType type;
    private int index;
    private int size;
    private String cloudStoreId;

    /**
     * Generates an ID for a slice.
     */
    public static String generateId() {
        return ObjectId.get().toString();
    }

    /**
     * Returns the DB ID of this slice.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the DB ID of this slice.
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * Returns the ID of the data from where this slice was obtained.
     */
    public String getDataId() {
        return dataId;
    }

    /**
     * Sets the ID of the data from where this slice was obtained.
     */
    public void setDataId(final String dataId) {
        this.dataId = dataId;
    }

    /**
     * Returns the slice type (data or coding).
     */
    public SliceType getType() {
        return type;
    }

    /**
     * Sets the slice type (data or coding).
     */
    public void setType(final SliceType type) {
        this.type = type;
    }

    /**
     * Returns data or coding index of the slice.
     */
    public int getIndex() {
        return index;
    }

    /**
     * Sets data or coding index of the slice.
     */
    public void setIndex(final int index) {
        this.index = index;
    }

    /**
     * Returns the size of the slice.
     */
    public int getSize() {
        return size;
    }

    /**
     * Sets the size of the slice.
     */
    public void setSize(final int size) {
        this.size = size;
    }

    /**
     * Returns the ID of the cloud store where this slice resides.
     */
    public String getCloudStoreId() {
        return cloudStoreId;
    }

    /**
     * Sets the ID of the cloud store where this slice resides.
     */
    public void setCloudStoreId(final String cloudStoreId) {
        this.cloudStoreId = cloudStoreId;
    }

    /**
     * Returns the name of the slice, which is the combination of the slice type prefix and the slice index (e.g d1,
     * c3).
     */
    public String getName() {
        return type.getPrefix() + index;
    }

    @Override
    public String toString() {
        return "SliceMetadata{" +
            "id='" + id + '\'' +
            ", dataId='" + dataId + '\'' +
            ", type=" + type +
            ", index=" + index +
            ", size=" + size +
            ", cloudStoreId='" + cloudStoreId + '\'' +
            '}';
    }

}
