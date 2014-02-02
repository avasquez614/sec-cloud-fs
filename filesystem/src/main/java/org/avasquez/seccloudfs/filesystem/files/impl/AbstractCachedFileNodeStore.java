package org.avasquez.seccloudfs.filesystem.files.impl;

import org.avasquez.seccloudfs.filesystem.files.User;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;

import java.io.IOException;

/**
 * Created by alfonsovasquez on 19/01/14.
 */
public abstract class AbstractCachedFileNodeStore implements FileNodeStore {

    public static final String FILE_CACHE_NAME = "files";

    private Cache<String, FileNode> cache;

    public void setCacheContainer(CacheContainer cacheContainer) {
        cache = cacheContainer.getCache(FILE_CACHE_NAME);
        if (cache == null) {
            throw new IllegalArgumentException("No '" + FILE_CACHE_NAME + "' cache found");
        }
    }

    @Override
    public FileNode find(String id) throws IOException {
        FileNode file;

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
    public FileNode create(boolean dir, User owner, long permissions) throws IOException {
        FileNode file = doCreate(dir, owner, permissions);

        cache.put(file.getId(), file);

        return file;
    }

    @Override
    public void delete(FileNode file) throws IOException {
        doDelete(file);

        cache.remove(file.getId());
    }

    protected abstract FileNode doFind(String id) throws IOException;
    protected abstract FileNode doCreate(boolean dir, User owner, long permissions) throws IOException;
    protected abstract void doDelete(FileNode file) throws IOException;

}
