package org.avasquez.seccloudfs.filesystem.content.impl;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import org.avasquez.seccloudfs.filesystem.content.Content;
import org.avasquez.seccloudfs.filesystem.content.ContentStore;
import org.avasquez.seccloudfs.filesystem.util.CacheUtils;

import java.io.IOException;

/**
 * Created by alfonsovasquez on 11/01/14.
 */
public abstract class AbstractCachedContentStore implements ContentStore {

    public static final String CONTENT_CACHE_NAME = "content";

    private Cache cache;

    public void setCacheManager(CacheManager cacheManager) {
        cache = cacheManager.getCache(CONTENT_CACHE_NAME);
        if (cache == null) {
            throw new IllegalArgumentException("No '" + CONTENT_CACHE_NAME + "' cache found");
        }
    }

    @Override
    public Content find(String id) throws IOException {
        Content content;

        if ((content = CacheUtils.get(cache, id)) != null) {
            return content;
        } else {
            synchronized (this) {
                if ((content = CacheUtils.get(cache, id)) != null) {
                    return content;
                }

                content = doFind(id);

                CacheUtils.put(cache, id, content);

                return content;
            }
        }
    }

    @Override
    public Content create() throws IOException {
        Content content = doCreate();

        CacheUtils.put(cache, content.getId(), content);

        return content;
    }

    @Override
    public void delete(String id) throws IOException {
        doDelete(id);

        cache.remove(id);
    }

    protected abstract Content doFind(String id) throws IOException;
    protected abstract Content doCreate() throws IOException;
    protected abstract void doDelete(String id) throws IOException;

}
