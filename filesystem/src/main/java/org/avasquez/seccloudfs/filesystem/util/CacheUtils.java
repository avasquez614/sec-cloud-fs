package org.avasquez.seccloudfs.filesystem.util;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
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
    public static <T> T get(Cache cache, String key) {
        Element element;
        if ((element = cache.get(key)) != null) {
            logger.debug("Object found in cache {} for key {}", cache.getName(), key);

            return (T) element.getObjectValue();
        } else {
            logger.debug("Object not found in cache {} for key {}", cache.getName(), key);

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
    public static <T> void put(Cache cache, String key, T value) {
        if (value != null) {
            cache.put(new Element(key, value));

            logger.debug("Object put in cache {} for key {}", cache.getName(), key);
        }
    }

}
