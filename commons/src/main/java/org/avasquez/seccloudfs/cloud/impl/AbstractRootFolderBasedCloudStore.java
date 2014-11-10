package org.avasquez.seccloudfs.cloud.impl;

import java.io.IOException;
import javax.annotation.PostConstruct;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.cloud.CloudStoreRegistrar;
import org.avasquez.seccloudfs.cloud.CloudStoreRegistry;
import org.avasquez.seccloudfs.db.Repository;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.utils.adapters.ClientFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * From all credentials stored in a credentials repository, or with a a set of credentials provided, creates a number
 * of {@link org.avasquez.seccloudfs.cloud.CloudStore}s for each set of credentials, based on different folders.
 *
 * @author avasquez
 */
public abstract class AbstractRootFolderBasedCloudStore<K, C> implements CloudStoreRegistrar {

    private Repository<C> credentialsRepository;
    private Iterable<C> credentials;
    private ClientFactory<K, C> clientFactory;
    private String rootFolderNameFormat;
    private int storesPerAccount;

    public void setCredentialsRepository(Repository<C> credentialsRepository) {
        this.credentialsRepository = credentialsRepository;
    }

    public void setCredentials(Iterable<C> credentials) {
        this.credentials = credentials;
    }

    @Required
    public void setClientFactory(ClientFactory<K, C> clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Required
    public void setRootFolderNameFormat(String rootFolderNameFormat) {
        this.rootFolderNameFormat = rootFolderNameFormat;
    }

    @Required
    public void setStoresPerAccount(int storesPerAccount) {
        this.storesPerAccount = storesPerAccount;
    }

    @PostConstruct
    public void init() throws IOException {
        if (credentials == null && credentialsRepository != null) {
            credentials = findCredentials();
        }
    }

    @Override
    public void registerStores(CloudStoreRegistry registry) throws IOException {
        if (credentials != null) {
            for (C cred : credentials) {
                K client = clientFactory.createClient(cred);

                for (int i = 1; i <= storesPerAccount; i++) {
                    String rootFolderName = String.format(rootFolderNameFormat, i);

                    registry.register(createStore(client, cred, rootFolderName));
                }
            }
        }
    }

    protected abstract CloudStore createStore(K client, C credentials, String rootFolderName) throws IOException;

    private Iterable<C> findCredentials() throws IOException {
        try {
            return credentialsRepository.findAll();
        } catch (DbException e) {
            throw new IOException("Unable to retrieve all saved accounts", e);
        }
    }

}
