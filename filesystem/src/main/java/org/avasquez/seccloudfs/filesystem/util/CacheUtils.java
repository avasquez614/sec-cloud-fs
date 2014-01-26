package org.avasquez.seccloudfs.filesystem.util;

import org.infinispan.Cache;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods for cache related stuff.
 *
 * @author avasquez
 */
public class CacheUtils {

    private static final Logger logger = Logger.getLogger(CacheUtils.class.getName());

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
            logger.log(Level.FINEST, "Cache hit: cache '{0}', key '{1}'", new Object[] {cache.getName(), key});

            return value;
        } else {
            logger.log(Level.FINEST, "Cache miss: cache '{0}', key '{1}'", new Object[] {cache.getName(), key});

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

            logger.log(Level.FINEST, "Cache put: cache '{0}', key '{1}'", new Object[] {cache.getName(), key});
        }
    }

}
