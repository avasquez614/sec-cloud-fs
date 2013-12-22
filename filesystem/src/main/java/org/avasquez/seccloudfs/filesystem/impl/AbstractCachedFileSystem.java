package org.avasquez.seccloudfs.filesystem.impl;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import org.avasquez.seccloudfs.filesystem.File;
import org.avasquez.seccloudfs.filesystem.FileSystem;
import org.avasquez.seccloudfs.filesystem.exception.FileExistsException;
import org.avasquez.seccloudfs.filesystem.exception.FileSystemException;
import org.avasquez.seccloudfs.filesystem.util.CacheUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Abstract {@link org.avasquez.seccloudfs.filesystem.FileSystem} that handles boilerplate cache code.
 *
 * @author avasquez
 */
public abstract class AbstractCachedFileSystem implements FileSystem {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCachedFileSystem.class);

    public static final String FILE_CACHE_NAME =            "files";
    public static final String FILE_CACHE_KEY_FORMAT =      "file[%s]";
    public static final String CHILDREN_CACHE_KEY_FORMAT =  "children[%s]";

    private long maxFilesInCache;
    private long cachedFileExpirationTime;
    private CacheManager cacheManager;
    private Cache cache;

    public void setMaxFilesInCache(long maxFilesInCache) {
        this.maxFilesInCache = maxFilesInCache;
    }

    public void setCachedFileExpirationTime(long cachedFileExpirationTime) {
        this.cachedFileExpirationTime = cachedFileExpirationTime;
    }

    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @PostConstruct
    public void init() {
        CacheConfiguration config = new CacheConfiguration();
        config.setName(FILE_CACHE_NAME);
        config.setMaxEntriesInCache(maxFilesInCache);
        config.setTimeToIdleSeconds(cachedFileExpirationTime);

        cacheManager.addCache(new Cache(config));

        cache = cacheManager.getCache(FILE_CACHE_NAME);
    }

    @Override
    public File getFile(String path) throws FileSystemException {
        String key = getFileKey(path);
        File file;

        if ((file = CacheUtils.get(cache, key)) != null) {
            return file;
        } else {
            synchronized (this) {
                if ((file = CacheUtils.get(cache, key)) != null) {
                    return file;
                }

                file = doGetFile(path);
                CacheUtils.put(cache, key, file);

                return file;
            }
        }
    }

    @Override
    public File createFile(String path, boolean dir) throws FileSystemException {
        String key = getFileKey(path);

        if (!cache.isKeyInCache(key)) {
            synchronized (this) {
                if (!cache.isKeyInCache(key)) {
                    File file = doCreateFile(path, dir);
                    updateCacheOnCreate(file);

                    return file;
                }
            }
        }

        throw new FileExistsException(String.format("File %s already exists", path));
    }

    @Override
    public List<File> getChildren(String path) throws FileSystemException {
        String key = getChildrenKey(path);
        List<File> children;

        if ((children = CacheUtils.get(cache, key)) != null) {
            return children;
        } else {
            synchronized (this) {
                if ((children = CacheUtils.get(cache, key)) != null) {
                    return children;
                }

                children = doGetChildren(path);
                CacheUtils.put(cache, key, children);

                // Put the individual children in the cache, if not already in cache
                if (children != null) {
                    for (File child : children) {
                        String childKey = getFileKey(child.getPath());
                        if (!cache.isKeyInCache(childKey)) {
                            CacheUtils.put(cache, childKey, child);
                        }
                    }
                }

                return children;
            }
        }
    }

    @Override
    public synchronized void deleteFile(String path) throws FileSystemException {
        File file = CacheUtils.get(cache, getFileKey(path));
        updateCacheOnDelete(file);

        doDeleteFile(path);
    }

    @Override
    public synchronized File copyFile(String srcPath, String dstPath) throws FileSystemException {
        File copiedFile = doCopyFile(srcPath, dstPath);
        updateCacheOnCreate(copiedFile);

        return copiedFile;
    }

    @Override
    public synchronized File moveFile(String srcPath, String dstPath) throws FileSystemException {
        File newFile = doMoveFile(srcPath, dstPath);
        updateCacheOnCreate(newFile);

        File oldFile = CacheUtils.get(cache, getFileKey(srcPath));
        updateCacheOnDelete(oldFile);

        return newFile;
    }

    protected abstract File doGetFile(String path) throws FileSystemException;
    protected abstract File doCreateFile(String path, boolean dir) throws FileSystemException;
    protected abstract List<File> doGetChildren(String path) throws FileSystemException;
    protected abstract void doDeleteFile(String path) throws FileSystemException;
    protected abstract File doCopyFile(String srcPath, String dstPath) throws FileSystemException;
    protected abstract File doMoveFile(String srcPath, String dstPath) throws FileSystemException;

    protected String getFileKey(String path) {
        return String.format(FILE_CACHE_KEY_FORMAT, path);
    }

    protected String getChildrenKey(String path) {
        return String.format(CHILDREN_CACHE_KEY_FORMAT, path);
    }

    protected void updateCacheOnCreate(File file) {
        if (file != null) {
            CacheUtils.put(cache, getFileKey(file.getPath()), file);

            // Update the cached parent's children
            String childrenKey = getChildrenKey(file.getParent());
            List<File> children = CacheUtils.get(cache, childrenKey);

            if (children != null) {
                children.add(file);
            }
        }
    }

    protected void updateCacheOnDelete(File file) {
        if (file != null) {
            // Remove itself from the cached parent's children
            String childrenKey = getChildrenKey(file.getParent());
            List<File> children = CacheUtils.get(cache, childrenKey);

            if (children != null) {
                children.remove(file);
            }

            cache.remove(getFileKey(file.getPath()));
        }
    }

}
