package org.avasquez.seccloudfs.cloud;

import java.io.IOException;

/**
 * Registers {@link CloudStore}s with a {@link org.avasquez.seccloudfs.cloud.CloudStoreRegistry}.
 *
 * @author avasquez
 */
public interface CloudStoreRegistrar {

    /**
     * Registers {@link CloudStore}s with a {@link org.avasquez.seccloudfs.cloud.CloudStoreRegistry}.
     *
     * @param registry the registry to register the stores to
     */
    void registerStores(CloudStoreRegistry registry) throws IOException;

}
