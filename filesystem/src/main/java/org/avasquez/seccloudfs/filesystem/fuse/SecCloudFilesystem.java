package org.avasquez.seccloudfs.filesystem.fuse;

import net.fusejna.util.FuseFilesystemAdapterFull;
import org.apache.commons.lang.StringUtils;
import org.avasquez.seccloudfs.filesystem.exception.PermissionDeniedException;
import org.avasquez.seccloudfs.filesystem.files.File;
import org.avasquez.seccloudfs.filesystem.files.FileStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static net.fusejna.ErrorCodes.*;
import static net.fusejna.StructFuseFileInfo.FileInfoWrapper;
import static net.fusejna.StructStat.StatWrapper;
import static net.fusejna.types.TypeMode.NodeType;

/**
 * Created by alfonsovasquez on 25/01/14.
 */
public class SecCloudFilesystem extends FuseFilesystemAdapterFull {

    /**
     * Separator for file path components.
     */
    public static final String PATH_SEPARATOR = "/";

    private static final Logger logger = LoggerFactory.getLogger(SecCloudFilesystem.class);

    private FileStore fileStore;

    public void setFileStore(FileStore fileStore) {
        this.fileStore = fileStore;
    }

    @Override
    public int getattr(final String path, final StatWrapper stat) {
        return doWithLogging(new LoggedMethod() {

            @Override
            public int invoke() throws PermissionDeniedException, IOException {
                File file = resolveFile(path);
                if (file != null) {
                    checkReadPermission(file);

                    NodeType nodeType = file.isDirectory()? NodeType.DIRECTORY : NodeType.FILE;
                    long permissions = file.getPermissions();
                    long mode = nodeType.getBits() | permissions;

                    stat.mode(mode);
                    stat.uid(file.getOwner().getUid());
                    stat.gid(file.getOwner().getGid());
                    stat.size(file.getSize());
                    stat.ctime(millisToSeconds(file.getLastChangeTime().getTime()));
                    stat.atime(millisToSeconds(file.getLastAccessTime().getTime()));
                    stat.mtime(millisToSeconds(file.getLastModifiedTime().getTime()));

                    return 0;
                } else {
                    return -ENOENT();
                }
            }

        }, "getattr", path, stat);
    }

    @Override
    public int access(final String path, final int access) {
        return doWithLogging(new LoggedMethod() {

            @Override
            public int invoke() throws PermissionDeniedException, IOException {
                File file = resolveFile(path);
                if (file != null) {
                    if (access == Constants.F_OK || hasPermission(file, access)) {
                        return 0;
                    } else {
                        return -EACCES();
                    }
                } else {
                    return -ENOENT();
                }
            }

        }, "access", path, access);
    }

    @Override
    public int opendir(final String path, final FileInfoWrapper info) {
        return doWithLogging(new LoggedMethod() {

            @Override
            public int invoke() throws PermissionDeniedException, IOException {
                File file = resolveFile(path);
                if (file != null) {
                    checkReadPermission(file);

                    return 0;
                } else {
                    return -ENOENT();
                }
            }

        }, "opendir", path, info);
    }

    private int doWithLogging(LoggedMethod method, String methodName, Object... args) {
        log(methodName, args);

        try {
            return method.invoke();
        } catch (PermissionDeniedException e) {
            logError(e, methodName, args);

            return -EACCES();
        } catch (IOException e) {
            logError(e, methodName, args);

            return -EIO();
        }
    }

    private void log(String methodName, Object... args) {
        if (logger.isDebugEnabled()) {
            int uid = getCurrentUid();
            int gid = getCurrentGid();

            logger.debug("{}({}) called by user[uid='{}',gid='{}']", methodName,
                    StringUtils.join(args, ','), uid, gid);
        }
    }

    private void logError(Throwable ex, String methodName, Object... args) {
        int uid = getCurrentUid();
        int gid = getCurrentGid();

        logger.error(String.format("%s(%s) call by user[uid='%s',gid='%s'] failed",
                methodName, StringUtils.join(args, ','), uid, gid), ex);
    }

    private long millisToSeconds(long millis) {
        return TimeUnit.MILLISECONDS.toSeconds(millis);
    }

    private File resolveFile(String path) throws PermissionDeniedException, IOException {
        return resolveFile(fileStore.getRoot(), path);
    }

    private File resolveFile(File currentFile, String path) throws PermissionDeniedException, IOException {
        path = StringUtils.strip(path, PATH_SEPARATOR);

        if (path.isEmpty()) {
            return currentFile;
        }

        // Execute permission means the user can search the directory
        checkExecutePermission(currentFile);

        int indexOfSep = path.indexOf(PATH_SEPARATOR);
        if (indexOfSep < 0) {
            return currentFile.getChild(path);
        } else {
            File nextFile = currentFile.getChild(path.substring(0, indexOfSep));
            if (nextFile != null) {
                return resolveFile(nextFile, path.substring(indexOfSep));
            }
        }

        return null;
    }

    private int getCurrentUid() {
        return getFuseContextUid().intValue();
    }

    private int getCurrentGid() {
        return getFuseContextGid().intValue();
    }

    private boolean hasPermission(File file, int permission) {
        if (file.getOwner().getUid() == getCurrentUid()) {
            return (file.getPermissions() & (permission << 6)) > 0;
        } else if (file.getOwner().getGid() == getCurrentGid()) {
            return (file.getPermissions() & (permission << 3)) > 0;
        } else {
            return (file.getPermissions() & permission) > 0;
        }
    }

    private void checkReadPermission(File file) throws PermissionDeniedException {
        if (!hasPermission(file, Constants.R_OK)) {
            throw new PermissionDeniedException("Read denied");
        }
    }

    private void checkWritePermission(File file) throws PermissionDeniedException {
        if (!hasPermission(file, Constants.W_OK)) {
            throw new PermissionDeniedException("Write denied");
        }
    }

    private void checkExecutePermission(File file) throws PermissionDeniedException {
        if (!hasPermission(file, Constants.X_OK)) {
            throw new PermissionDeniedException("Execute or search denied");
        }
    }

    private static interface LoggedMethod {

        public int invoke() throws PermissionDeniedException, IOException;

    }

}
