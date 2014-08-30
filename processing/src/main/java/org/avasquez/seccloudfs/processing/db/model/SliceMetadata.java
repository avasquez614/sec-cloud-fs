package org.avasquez.seccloudfs.processing.db.model;

import org.bson.types.ObjectId;

/**
 * The slice metadata, stored in a {@link org.avasquez.seccloudfs.cloud.CloudStore} to be used for later fragment
 * retrieval
 *
 * @author avasquez
 */
public class SliceMetadata {

    private String id;
    private String cloudStoreName;

    /**
     * Generates an ID for a slice.
     */
    public static String generateId() {
        return ObjectId.get().toString();
    }

    /**
     * Returns the ID of this slice.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the ID of this slice.
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * Returns the name of the cloud store where this slice resides.
     */
    public String getCloudStoreName() {
        return cloudStoreName;
    }

    /**
     * Sets the name of the cloud store where this slice resides.
     */
    public void setCloudStoreName(final String cloudStoreName) {
        this.cloudStoreName = cloudStoreName;
    }

}
