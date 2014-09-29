package org.avasquez.seccloudfs.processing.utils.crypto;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.processing.db.repos.EncryptionKeyRepository;
import org.avasquez.seccloudfs.utils.DecoratorFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * {@link org.avasquez.seccloudfs.utils.DecoratorFactory} that decorates any {@link org.avasquez.seccloudfs.cloud
 * .CloudStore} with an {@link org.avasquez.seccloudfs.processing.utils.crypto.EncryptingCloudStore}.
 *
 * @author avasquez
 */
public class EncryptingCloudStoreDecoratorFactory implements DecoratorFactory<CloudStore> {

    private EncryptionKeyRepository keyRepository;
    private String tmpDir;

    @Required
    public void setKeyRepository(EncryptionKeyRepository keyRepository) {
        this.keyRepository = keyRepository;
    }

    @Required
    public void setTmpDir(String tmpDir) {
        this.tmpDir = tmpDir;
    }

    @Override
    public CloudStore decorate(CloudStore cloudStore) {
        EncryptingCloudStore encryptingCloudStore = new EncryptingCloudStore();
        encryptingCloudStore.setUnderlyingStore(cloudStore);
        encryptingCloudStore.setKeyRepository(keyRepository);
        encryptingCloudStore.setTmpDir(tmpDir);

        return encryptingCloudStore;
    }

}
