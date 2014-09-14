package org.avasquez.seccloudfs.cloud;

import java.io.IOException;

/**
 * Registers {@link org.avasquez.seccloudfs.cloud.CloudStore}s with a {@link org.avasquez.seccloudfs.cloud
 * .CloudStoreRegistry}.
 *
 * @author avasquez
 */
public interface CloudStoreRegistrar {

    /**
     * Registers {@link org.avasquez.seccloudfs.cloud.CloudStore}s with the specified registry.
     *
     * @param registry the registry
     */
    void registerStores(CloudStoreRegistry registry) throws IOException;

}
