package org.avasquez.seccloudfs.filesystem.files.impl;

import java.io.IOException;

import org.avasquez.seccloudfs.filesystem.files.User;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.springframework.beans.factory.annotation.Required;

/**
 * Created by alfonsovasquez on 19/01/14.
 */
public abstract class AbstractCachedFileObjectStore implements FileObjectStore {

    public static final String FILE_NODE_CACHE_NAME = "fileNodes";

    protected Cache<String, FileObject> cache;

    @Required
    public void setCacheContainer(CacheContainer cacheContainer) {
        cache = cacheContainer.getCache(FILE_NODE_CACHE_NAME);
        if (cache == null) {
            throw new IllegalArgumentException("No '" + FILE_NODE_CACHE_NAME + "' cache found");
        }
    }

    @Override
    public FileObject find(String id) throws IOException {
        FileObject file;

        if ((file = cache.get(id)) != null) {
            return file;
        } else {
            synchronized (this) {
                if ((file = cache.get(id)) != null) {
                    return file;
                }

                file = doFind(id);
                if (file != null) {
                    cache.put(id, file);
                }

                return file;
            }
        }
    }

    @Override
    public FileObject create(boolean dir, User owner, long permissions) throws IOException {
        FileObject file = doCreate(dir, owner, permissions);

        cache.put(file.getId(), file);

        return file;
    }

    @Override
    public void delete(FileObject file) throws IOException {
        doDelete(file);

        cache.remove(file.getId());
    }

    protected abstract FileObject doFind(String id) throws IOException;
    protected abstract FileObject doCreate(boolean dir, User owner, long permissions) throws IOException;
    protected abstract void doDelete(FileObject file) throws IOException;

}
