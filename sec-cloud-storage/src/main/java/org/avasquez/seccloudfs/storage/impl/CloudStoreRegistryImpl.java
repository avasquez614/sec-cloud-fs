package org.avasquez.seccloudfs.storage.impl;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.storage.CloudStoreRegistry;

/**
 * Default implementation of {@link org.avasquez.seccloudfs.storage.CloudStoreRegistry}.
 *
 * @author avasquez
 */
public class CloudStoreRegistryImpl implements CloudStoreRegistry {

    private Map<String, LastUploadTimeAwareCloudStore> stores;

    public void setStores(final Map<String, CloudStore> stores) {
        this.stores = new HashMap<>(stores.size());

        for (Map.Entry<String, CloudStore> entry : stores.entrySet()) {
            this.stores.put(entry.getKey(), new LastUploadTimeAwareCloudStore(entry.getValue()));
        }
    }

    /**
     * Returns the list of available {@link org.avasquez.seccloudfs.cloud.CloudStore}s, ordered by their last
     * upload time.
     */
    @Override
    public Iterable<CloudStore> list() {
        List<CloudStore> list = new ArrayList<CloudStore>(stores.values());

        Collections.sort(list, new Comparator<CloudStore>() {

            @Override
            public int compare(final CloudStore store1, final CloudStore store2) {
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
    public CloudStore find(final String id) {
        return stores.get(id);
    }

    private static class LastUploadTimeAwareCloudStore implements CloudStore {

        private CloudStore underlyingStore;
        private volatile long lastUploadTime;

        private LastUploadTimeAwareCloudStore(final CloudStore underlyingStore) {
            this.underlyingStore = underlyingStore;
        }

        public long getLastUploadTime() {
            return lastUploadTime;
        }

        @Override
        public long upload(final String id, final ReadableByteChannel src, final long length) throws IOException {
            long bytesUploaded = underlyingStore.upload(id, src, length);

            lastUploadTime = System.currentTimeMillis();

            return bytesUploaded;
        }

        @Override
        public long download(final String id, final WritableByteChannel target) throws IOException {
            return underlyingStore.download(id, target);
        }

        @Override
        public void delete(final String id) throws IOException {
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
