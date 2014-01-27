package org.avasquez.seccloudfs.filesystem.files.impl;

import org.avasquez.seccloudfs.filesystem.files.File;
import org.avasquez.seccloudfs.filesystem.files.FileStore;
import org.avasquez.seccloudfs.filesystem.files.User;
import org.avasquez.seccloudfs.filesystem.util.CacheUtils;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;

import java.io.IOException;

/**
 * Created by alfonsovasquez on 19/01/14.
 */
public abstract class AbstractCachedFileStore implements FileStore {

    public static final String FILE_CACHE_NAME =    "files";

    private volatile File root;
    private Cache<String, File> cache;

    public void setCacheContainer(CacheContainer cacheContainer) {
        cache = cacheContainer.getCache(FILE_CACHE_NAME);
        if (cache == null) {
            throw new IllegalArgumentException("No '" + FILE_CACHE_NAME + "' cache found");
        }
    }

    @Override
    public File getRoot() throws IOException {
        if (root == null) {
            synchronized (this) {
                if (root == null) {
                    root = doGetRoot();
                }
            }
        }

        return root;
    }

    @Override
    public File find(String id) throws IOException {
        File file;

        if ((file = CacheUtils.get(cache, id)) != null) {
            return file;
        } else {
            synchronized (this) {
                if ((file = CacheUtils.get(cache, id)) != null) {
                    return file;
                }

                file = doFind(id);
                CacheUtils.put(cache, id, file);

                return file;
            }
        }
    }



    @Override
    public synchronized File create(File parent, String name, boolean dir, User owner, long permissions)
            throws IOException {
        File file = doCreate(parent, name, dir, owner, permissions);
        CacheUtils.put(cache, file.getId(), file);

        return file;
    }

    @Override
    public File move(File file, File newParent, String name) throws IOException {
        return doMove(file, newParent, name);
    }

    @Override
    public void delete(File file) throws IOException {
        doDelete(file);

        cache.remove(file.getId());
    }

    protected abstract File doGetRoot() throws IOException;
    protected abstract File doFind(String id) throws IOException;
    protected abstract File doCreate(File parent, String name, boolean dir, User owner, long permissions)
            throws IOException;
    protected abstract File doMove(File file, File newParent, String newName) throws IOException;
    protected abstract void doDelete(File file) throws IOException;

}
