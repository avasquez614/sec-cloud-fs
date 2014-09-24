package org.avasquez.seccloudfs.processing.utils.zip;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.utils.DecoratorFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * {@link org.avasquez.seccloudfs.utils.DecoratorFactory} that decorates any {@link org.avasquez.seccloudfs.cloud
 * .CloudStore} with a {@link org.avasquez.seccloudfs.processing.utils.zip.GZipCloudStore}.
 *
 * @author avasquez
 */
public class GZipCloudStoreDecoratorFactory implements DecoratorFactory<CloudStore> {

    private String tmpDir;

    @Required
    public void setTmpDir(String tmpDir) {
        this.tmpDir = tmpDir;
    }

    @Override
    public CloudStore decorate(CloudStore cloudStore) {
        GZipCloudStore gZipCloudStore = new GZipCloudStore();
        gZipCloudStore.setUnderlyingStore(cloudStore);
        gZipCloudStore.setTmpDir(tmpDir);

        return gZipCloudStore;
    }

}
