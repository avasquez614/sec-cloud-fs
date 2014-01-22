package org.avasquez.seccloudfs.filesystem.util;

import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for cache related stuff.
 *
 * @author avasquez
 */
public class CacheUtils {

    private static final Logger logger = LoggerFactory.getLogger(CacheUtils.class);

    private CacheUtils() {
    }

    /**
     * Returns the object from the cache associated to the given key.
     *
     * @param cache the cache where the object is stored
     * @param key   the object key
     *
     * @return the cached object, or null if not found
     */
    public static <K, V> V get(Cache<K, V> cache, K key) {
        V value;

        if ((value = cache.get(key)) != null) {
            logger.debug("Cache hit: cache '{}', key '{}'", cache.getName(), key);

            return value;
        } else {
            logger.debug("Cache miss: cache '{}', key '{}'", cache.getName(), key);

            return null;
        }
    }

    /**
     * Puts the object for the associated key in the cache.
     *
     * @param cache the cache where to put the object
     * @param key   the object key
     * @param value the object to cache. Can be null, in which case nothing is put in the cache
     */
    public static <K, V> void put(Cache<K, V> cache, K key, V value) {
        if (value != null) {
            cache.put(key, value);

            logger.debug("Cache put: cache '{}', key '{}'", cache.getName(), key);
        }
    }

}
