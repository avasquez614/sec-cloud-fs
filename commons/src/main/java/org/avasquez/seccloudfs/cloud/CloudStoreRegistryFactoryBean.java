package org.avasquez.seccloudfs.cloud;

import java.util.List;

import org.avasquez.seccloudfs.cloud.impl.CloudStoreRegistryImpl;
import org.springframework.beans.factory.FactoryBean;

/**
 * {@link org.springframework.beans.factory.FactoryBean} for creating a {@link org.avasquez.seccloudfs.cloud
 * .CloudStoreRegistry}.
 *
 * @author avasquez
 */
public class CloudStoreRegistryFactoryBean implements FactoryBean<CloudStoreRegistry> {

    private List<CloudStore> cloudStores;
    private List<CloudStoreRegistrar> cloudStoreRegistrars;

    public void setCloudStores(List<CloudStore> cloudStores) {
        this.cloudStores = cloudStores;
    }

    public void setCloudStoreRegistrars(List<CloudStoreRegistrar> cloudStoreRegistrars) {
        this.cloudStoreRegistrars = cloudStoreRegistrars;
    }

    @Override
    public CloudStoreRegistry getObject() throws Exception {
        CloudStoreRegistry registry = createCloudStoreRegistry();

        if (cloudStores != null) {
            for (CloudStore cloudStore : cloudStores) {
                registry.register(cloudStore);
            }
        }
        if (cloudStoreRegistrars != null) {
            for (CloudStoreRegistrar registrar : cloudStoreRegistrars) {
                registrar.registerStores(registry);
            }
        }

        return registry;
    }

    @Override
    public Class<?> getObjectType() {
        return CloudStoreRegistry.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    protected CloudStoreRegistry createCloudStoreRegistry() {
        return new CloudStoreRegistryImpl();
    }

}
