package org.avasquez.seccloudfs.cloud.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.cloud.CloudStoreRegistry;

/**
 * Default implementation of {@link org.avasquez.seccloudfs.cloud.CloudStoreRegistry}.
 *
 * @author avasquez
 */
public class CloudStoreRegistryImpl implements CloudStoreRegistry {

    private Map<String, CloudStore> stores;

    public CloudStoreRegistryImpl() {
        stores = new HashMap<>();
    }

    @Override
    public void register(CloudStore store) {
        stores.put(store.getName(), store);
    }

    @Override
    public Collection<CloudStore> list() {
        return new ArrayList<>(stores.values());
    }

    @Override
    public CloudStore find(String name) {
        return stores.get(name);
    }

}
