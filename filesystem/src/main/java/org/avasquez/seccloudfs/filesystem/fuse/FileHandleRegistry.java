package org.avasquez.seccloudfs.filesystem.fuse;

import org.avasquez.seccloudfs.filesystem.util.CacheUtils;
import org.avasquez.seccloudfs.filesystem.util.FlushableByteChannel;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntriesEvicted;
import org.infinispan.notifications.cachelistener.event.CacheEntriesEvictedEvent;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by alfonsovasquez on 26/01/14.
 */

public class FileHandleRegistry {

    private static Logger logger = Logger.getLogger(FileHandleRegistry.class.getName());

    private static final String FILE_HANDLE_CACHE_NAME = "fileHandles";

    private Cache<Long, FlushableByteChannel> cache;

    private AtomicLong handleIdGenerator;

    public FileHandleRegistry() {
        handleIdGenerator = new AtomicLong();
    }

    public void setCacheContainer(CacheContainer cacheContainer) {
        cache = cacheContainer.getCache(FILE_HANDLE_CACHE_NAME);
        if (cache == null) {
            throw new IllegalArgumentException("No '" + FILE_HANDLE_CACHE_NAME + "' cache found");
        }

        cache.addListener(new CacheListener());
    }

    public FlushableByteChannel getHandle(long handleId) {
        return CacheUtils.get(cache, handleId);
    }

    public long addHandle(FlushableByteChannel handle) {
        long handleId = handleIdGenerator.getAndIncrement();

        CacheUtils.put(cache, handleId, handle);

        return handleId;
    }

    public FlushableByteChannel removeHandle(long handleId) throws IOException {
        return cache.remove(handleId);
    }

    @Listener
    public static class CacheListener {

        @CacheEntriesEvicted
        public void onCacheEviction(CacheEntriesEvictedEvent<String, FlushableByteChannel> event) {
            for (Map.Entry<String, FlushableByteChannel> entry : event.getEntries().entrySet()) {
                try {
                    entry.getValue().close();
                } catch (IOException e) {
                    logger.log(Level.FINE, "Unable to close handle " + entry.getKey() + " correctly", e);
                }
            }
        }

    }

}
