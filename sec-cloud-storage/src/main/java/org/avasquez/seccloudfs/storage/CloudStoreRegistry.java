package org.avasquez.seccloudfs.storage;

import org.avasquez.seccloudfs.cloud.CloudStore;

/**
 * Represents a registry {@link org.avasquez.seccloudfs.cloud.CloudStore}s.
 * 
 * @author avasquez
 */
public interface CloudStoreRegistry {

    /**
     * Returns the list of available {@link org.avasquez.seccloudfs.cloud.CloudStore}s for use. The list might be
     * ordered according to the usage priority of the stores.
     */
    Iterable<CloudStore> list();

    /**
     * Returns the {@link org.avasquez.seccloudfs.cloud.CloudStore} corresponding to the specific ID.
     *
     * @param id the ID of the store to look for
     *
     * @return the store, or null if not found
     */
    CloudStore find(String id);
    
}
