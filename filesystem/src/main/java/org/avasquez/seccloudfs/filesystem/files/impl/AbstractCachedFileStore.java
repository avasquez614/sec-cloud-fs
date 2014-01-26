package org.avasquez.seccloudfs.filesystem.files.impl;

import org.avasquez.seccloudfs.filesystem.files.File;
import org.avasquez.seccloudfs.filesystem.files.FileStore;
import org.avasquez.seccloudfs.filesystem.files.User;
import org.avasquez.seccloudfs.filesystem.util.CacheUtils;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;

import java.io.IOException;
import java.util.List;

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
    public List<File> findChildren(String id) throws IOException {
        List<File> children = doFindChildren(id);
        // Put the individual children in the cache, if not already in cache. If already in cache, replace the
        // one in the list with the cached one (to avoid having several instances).
        if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                File child = children.get(i);
                File cachedChild = CacheUtils.get(cache, child.getId());

                if (cachedChild != null) {
                    children.set(i, cachedChild);
                } else {
                    CacheUtils.put(cache, child.getId(), child);
                }
            }
        }

        return children;
    }

    @Override
    public synchronized File create(String parentId, String name, boolean dir, User owner, long permissions)
            throws IOException {
        File file = doCreate(parentId, name, dir, owner, permissions);
        CacheUtils.put(cache, file.getId(), file);

        return file;
    }

    @Override
    public File rename(String id, String newName) throws IOException {
        return doRename(id, newName);
    }

    @Override
    public File move(String id, String newParentId, String newName) throws IOException {
        return doMove(id, newParentId, newName);
    }

    @Override
    public void delete(String id) throws IOException {
        doDelete(id);

        cache.remove(id);
    }

    protected abstract File doGetRoot() throws IOException;
    protected abstract File doFind(String id) throws IOException;
    protected abstract List<File> doFindChildren(String id) throws IOException;
    protected abstract File doCreate(String parentId, String name, boolean dir, User owner, long permissions)
            throws IOException;
    protected abstract File doRename(String parentId, String newName) throws IOException;
    protected abstract File doMove(String id, String newParentId, String newName) throws IOException;
    protected abstract void doDelete(String id) throws IOException;

}
