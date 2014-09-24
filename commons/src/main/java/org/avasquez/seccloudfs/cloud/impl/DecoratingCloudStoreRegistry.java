package org.avasquez.seccloudfs.cloud.impl;

import java.util.Collection;
import java.util.List;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.cloud.CloudStoreRegistry;
import org.avasquez.seccloudfs.utils.DecoratorFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * {@link org.avasquez.seccloudfs.cloud.CloudStoreRegistry} decorator that decorates {@link org.avasquez.seccloudfs
 * .cloud.CloudStore}s on registry using {@link org.avasquez.seccloudfs.utils.DecoratorFactory}. Decorators will be
 * applied in the order they are listed.
 *
 * @author avasquez
 */
public class DecoratingCloudStoreRegistry implements CloudStoreRegistry {

    private CloudStoreRegistry actualRegistry;
    private List<DecoratorFactory<CloudStore>> decoratorFactories;

    @Required
    public void setActualRegistry(CloudStoreRegistry actualRegistry) {
        this.actualRegistry = actualRegistry;
    }

    @Required
    public void setDecoratorFactories(List<DecoratorFactory<CloudStore>> decoratorFactories) {
        this.decoratorFactories = decoratorFactories;
    }

    @Override
    public void register(CloudStore store) {
        for (DecoratorFactory<CloudStore> decoratorFactory : decoratorFactories) {
            store = decoratorFactory.decorate(store);
        }

        actualRegistry.register(store);
    }

    @Override
    public Collection<CloudStore> list() {
        return actualRegistry.list();
    }

    @Override
    public CloudStore find(String name) {
        return actualRegistry.find(name);
    }

}
