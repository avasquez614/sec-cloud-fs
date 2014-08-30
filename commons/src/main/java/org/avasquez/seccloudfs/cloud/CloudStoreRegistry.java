package org.avasquez.seccloudfs.cloud;

import java.util.Collection;

/**
 * Represents a registry {@link org.avasquez.seccloudfs.cloud.CloudStore}s.
 * 
 * @author avasquez
 */
public interface CloudStoreRegistry {

    /**
     * Registers a new {@link org.avasquez.seccloudfs.cloud.CloudStore}.
     *
     * @param store the store to register
     */
    void register(CloudStore store);

    /**
     * Returns the list of available {@link org.avasquez.seccloudfs.cloud.CloudStore}s for use. The list might be
     * ordered according to the usage priority of the stores.
     */
    Collection<CloudStore> list();

    /**
     * Returns the {@link org.avasquez.seccloudfs.cloud.CloudStore} corresponding to the specific name.
     *
     * @param name the name of the store to look for
     *
     * @return the store, or null if not found
     */
    CloudStore find(String name);
    
}
