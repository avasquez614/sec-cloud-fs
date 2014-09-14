package org.avasquez.seccloudfs.cloud.impl;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.cloud.CloudStoreRegistry;

/**
 * Default implementation of {@link org.avasquez.seccloudfs.cloud.CloudStoreRegistry}.
 *
 * @author avasquez
 */
public class CloudStoreRegistryImpl implements CloudStoreRegistry {

    private Map<String, LastUploadTimeAwareCloudStore> stores;

    public CloudStoreRegistryImpl() {
        stores = new HashMap<>();
    }

    public void setStores(Map<String, CloudStore> stores) {
        for (Map.Entry<String, CloudStore> entry : stores.entrySet()) {
            this.stores.put(entry.getKey(), new LastUploadTimeAwareCloudStore(entry.getValue()));
        }
    }

    @Override
    public void register(CloudStore store) {
        stores.put(store.getName(), new LastUploadTimeAwareCloudStore(store));
    }

    /**
     * Returns the list of available {@link org.avasquez.seccloudfs.cloud.CloudStore}s, ordered by their last
     * upload time.
     */
    @Override
    public Collection<CloudStore> list() {
        List<CloudStore> list = new ArrayList<CloudStore>(stores.values());

        Collections.sort(list, new Comparator<CloudStore>() {

            @Override
            public int compare(CloudStore store1, CloudStore store2) {
                long lastUploadTime1 = ((LastUploadTimeAwareCloudStore) store1).getLastUploadTime();
                long lastUploadTime2 = ((LastUploadTimeAwareCloudStore) store2).getLastUploadTime();

                if (lastUploadTime1 < lastUploadTime2) {
                    return -1;
                } else if (lastUploadTime1 == lastUploadTime2) {
                    return 0;
                } else {
                    return 1;
                }
            }

        });

        return list;
    }

    /**
     * Returns the {@link org.avasquez.seccloudfs.cloud.CloudStore} from the map of stores.
     */
    @Override
    public CloudStore find(String name) {
        return stores.get(name);
    }

    private static class LastUploadTimeAwareCloudStore implements CloudStore {

        private CloudStore underlyingStore;
        private volatile long lastUploadTime;

        private LastUploadTimeAwareCloudStore(CloudStore underlyingStore) {
            this.underlyingStore = underlyingStore;
        }

        public long getLastUploadTime() {
            return lastUploadTime;
        }

        @Override
        public String getName() {
            return underlyingStore.getName();
        }

        @Override
        public void upload(String id, ReadableByteChannel src, long length) throws IOException {
            underlyingStore.upload(id, src, length);

            lastUploadTime = System.currentTimeMillis();
        }

        @Override
        public void download(String id, WritableByteChannel target) throws IOException {
            underlyingStore.download(id, target);
        }

        @Override
        public void delete(String id) throws IOException {
            underlyingStore.delete(id);
        }

        @Override
        public long getTotalSpace() throws IOException {
            return underlyingStore.getTotalSpace();
        }

        @Override
        public long getAvailableSpace() throws IOException {
            return underlyingStore.getAvailableSpace();
        }

    }

}
