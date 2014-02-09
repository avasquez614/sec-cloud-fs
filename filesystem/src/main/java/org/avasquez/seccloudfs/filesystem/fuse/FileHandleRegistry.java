package org.avasquez.seccloudfs.filesystem.fuse;

import org.avasquez.seccloudfs.filesystem.util.FlushableByteChannel;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntriesEvicted;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntriesEvictedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by alfonsovasquez on 26/01/14.
 */
public class FileHandleRegistry {

    private static final Logger logger = LoggerFactory.getLogger(FileHandleRegistry.class);

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

    public FlushableByteChannel get(long id) {
        return cache.get(id);
    }

    public long add(FlushableByteChannel handle) {
        long id = handleIdGenerator.getAndIncrement();

        cache.put(id, handle);

        return id;
    }

    public FlushableByteChannel destroy(long id) {
        return cache.remove(id);
    }

    public void destroyAll() {
        cache.clear();
    }

    @Listener
    public static class CacheListener {

        @CacheEntryRemoved
        public void onCacheRemove(CacheEntryRemovedEvent<Long, FlushableByteChannel> event) {
            closeHandle(event.getKey(), event.getValue());
        }

        @CacheEntriesEvicted
        public void onCacheEviction(CacheEntriesEvictedEvent<Long, FlushableByteChannel> event) {
            for (Map.Entry<Long, FlushableByteChannel> entry : event.getEntries().entrySet()) {
                closeHandle(entry.getKey(), entry.getValue());
            }
        }

        private void closeHandle(Long id, FlushableByteChannel handle) {
            try {
                if (handle.isOpen()) {
                    handle.close();

                    logger.debug("Handle {} closed", id);
                }
            } catch (IOException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Unable to close handle " + id + " correctly", e);
                }
            }
        }

    }

}
