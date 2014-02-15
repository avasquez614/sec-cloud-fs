package org.avasquez.seccloudfs.filesystem.content.impl;

import org.avasquez.seccloudfs.filesystem.content.Content;
import org.avasquez.seccloudfs.filesystem.content.ContentStore;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.springframework.beans.factory.annotation.Required;

import java.io.IOException;

/**
 * Created by alfonsovasquez on 11/01/14.
 */
public abstract class AbstractCachedContentStore implements ContentStore {

    public static final String CONTENT_CACHE_NAME = "content";

    private Cache<String, Content> cache;

    @Required
    public void setCacheContainer(CacheContainer cacheContainer) {
        cache = cacheContainer.getCache(CONTENT_CACHE_NAME);
        if (cache == null) {
            throw new IllegalArgumentException("No '" + CONTENT_CACHE_NAME + "' cache found");
        }
    }

    @Override
    public Content find(String id) throws IOException {
        Content content;

        if ((content = cache.get(id)) != null) {
            return content;
        } else {
            synchronized (this) {
                if ((content = cache.get(id)) != null) {
                    return content;
                }

                content = doFind(id);
                if (content != null) {
                    cache.put(id, content);
                }

                return content;
            }
        }
    }

    @Override
    public Content create() throws IOException {
        Content content = doCreate();

        cache.put(content.getId(), content);

        return content;
    }

    @Override
    public void delete(Content content) throws IOException {
        doDelete(content);

        cache.remove(content.getId());
    }

    protected abstract Content doFind(String id) throws IOException;
    protected abstract Content doCreate() throws IOException;
    protected abstract void doDelete(Content content) throws IOException;

}
