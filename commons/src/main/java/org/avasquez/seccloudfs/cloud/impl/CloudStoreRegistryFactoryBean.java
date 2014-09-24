package org.avasquez.seccloudfs.cloud.impl;

import java.util.List;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.cloud.CloudStoreRegistrar;
import org.avasquez.seccloudfs.cloud.CloudStoreRegistry;
import org.avasquez.seccloudfs.utils.DecoratorFactory;
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
    private List<DecoratorFactory<CloudStore>> decoratorFactories;

    public void setCloudStores(List<CloudStore> cloudStores) {
        this.cloudStores = cloudStores;
    }

    public void setCloudStoreRegistrars(List<CloudStoreRegistrar> cloudStoreRegistrars) {
        this.cloudStoreRegistrars = cloudStoreRegistrars;
    }

    public void setDecoratorFactories(List<DecoratorFactory<CloudStore>> decoratorFactories) {
        this.decoratorFactories = decoratorFactories;
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
        if (decoratorFactories != null && !decoratorFactories.isEmpty()) {
            DecoratingCloudStoreRegistry registry = new DecoratingCloudStoreRegistry();
            registry.setActualRegistry(new CloudStoreRegistryImpl());
            registry.setDecoratorFactories(decoratorFactories);

            return registry;
        } else {
            return new CloudStoreRegistryImpl();
        }
    }

}
