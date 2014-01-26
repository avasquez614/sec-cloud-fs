package org.avasquez.seccloudfs.filesystem.fuse;

import net.fusejna.DirectoryFiller;
import net.fusejna.util.FuseFilesystemAdapterFull;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.avasquez.seccloudfs.filesystem.exception.FileExistsException;
import org.avasquez.seccloudfs.filesystem.exception.FileNotFoundException;
import org.avasquez.seccloudfs.filesystem.exception.NotADirectoryException;
import org.avasquez.seccloudfs.filesystem.exception.PermissionDeniedException;
import org.avasquez.seccloudfs.filesystem.files.File;
import org.avasquez.seccloudfs.filesystem.files.FileStore;
import org.avasquez.seccloudfs.filesystem.files.User;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.fusejna.ErrorCodes.*;
import static net.fusejna.StructStat.StatWrapper;
import static net.fusejna.types.TypeMode.ModeWrapper;
import static net.fusejna.types.TypeMode.NodeType;

/**
 * Created by alfonsovasquez on 25/01/14.
 */
public class SecCloudFilesystem extends FuseFilesystemAdapterFull {

    /**
     * Separator for file path components.
     */
    public static final String PATH_SEPARATOR = "/";

    private static final Logger logger = Logger.getLogger(SecCloudFilesystem.class.getName());

    private FileStore fileStore;

    public void setFileStore(FileStore fileStore) {
        this.fileStore = fileStore;
    }

    @Override
    public int getattr(final String path, final StatWrapper stat) {
        return doWithErrorHandling(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                File file = resolveFile(path);

                checkReadPermission(file);

                NodeType nodeType = file.isDirectory() ? NodeType.DIRECTORY : NodeType.FILE;
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
            }

        }, "getattr");
    }

    @Override
    public int access(final String path, final int access) {
        return doWithErrorHandling(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                File file = resolveFile(path);

                if (access == Constants.F_OK || hasPermission(file, access)) {
                    return 0;
                } else {
                    return -EACCES();
                }
            }

        }, "access");
    }

    @Override
    public int readdir(final String path, final DirectoryFiller filler) {
        return doWithErrorHandling(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                File dir = resolveFile(path);

                checkIsDirectory(dir);
                checkReadPermission(dir);

                for (File child : dir.getChildren()) {
                    filler.add(child.getName());
                }

                return 0;
            }

        }, "readdir");
    }

    @Override
    public int mkdir(final String path, final ModeWrapper mode) {
        return doWithErrorHandling(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                File parent = resolveParent(path);

                checkIsDirectory(parent);
                checkWritePermission(parent);

                User owner = new User(getCurrentUid(), getCurrentGid());
                long permissions = getPermissionsBits(mode.mode());

                fileStore.create(parent.getId(), getFilename(path), true, owner, permissions);

                return 0;
            }

        }, "mkdir");
    }

    @Override
    public int rmdir(final String path) {
        return doWithErrorHandling(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                File dir = resolveFile(path);

                checkIsDirectory(dir);
                checkWritePermission(dir.getParent());

                fileStore.delete(dir.getId());

                return 0;
            }

        }, "rmdir");
    }

    private int doWithErrorHandling(Callable<Integer> method, String methodName) {
        try {
            return method.call();
        } catch (FileNotFoundException e) {
            logError(e, methodName);

            return -ENOENT();
        } catch (NotADirectoryException e) {
            logError(e, methodName);

            return -ENOTDIR();
        } catch (PermissionDeniedException e) {
            logError(e, methodName);

            return -EACCES();
        } catch (FileExistsException e) {
            logError(e, methodName);

            return -EEXIST();
        } catch (Exception e) {
            logError(e, methodName);

            return -EIO();
        }
    }

    private void logError(Throwable ex, String methodName) {
        logger.logp(Level.FINE, getClass().getName(), methodName, "Operation failed", ex);
    }

    private long millisToSeconds(long millis) {
        return TimeUnit.MILLISECONDS.toSeconds(millis);
    }

    private File resolveFile(String path) throws PermissionDeniedException, IOException {
        return resolveFile(fileStore.getRoot(), path);
    }

    private File resolveFile(File currentDir, String path) throws PermissionDeniedException, IOException {
        path = StringUtils.strip(path, PATH_SEPARATOR);

        if (path.isEmpty()) {
            return currentDir;
        }

        // Execute permission means the user can search the directory
        checkExecutePermission(currentDir);

        int indexOfSep = path.indexOf(PATH_SEPARATOR);
        if (indexOfSep < 0) {
            return currentDir.getChild(path);
        } else {
            File nextDir = currentDir.getChild(path.substring(0, indexOfSep));
            if (nextDir != null) {
                return resolveFile(nextDir, path.substring(indexOfSep));
            }
        }

        throw new FileNotFoundException("Not file found at path " + path + " in dir '" + currentDir.getId() + "'");
    }

    private File resolveParent(String path) throws PermissionDeniedException, IOException {
        String parentPath = StringUtils.stripEnd(FilenameUtils.getPath(path), PATH_SEPARATOR);

        return resolveFile(parentPath);
    }

    private String getFilename(String path) {
        return StringUtils.stripEnd(FilenameUtils.getName(path), PATH_SEPARATOR);
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
            throw new PermissionDeniedException("Read denied for '" + file.getId() + "'");
        }
    }

    private void checkWritePermission(File file) throws PermissionDeniedException {
        if (!hasPermission(file, Constants.W_OK)) {
            throw new PermissionDeniedException("Write denied '" + file.getId() + "'");
        }
    }

    private void checkExecutePermission(File file) throws PermissionDeniedException {
        if (!hasPermission(file, Constants.X_OK)) {
            throw new PermissionDeniedException("Execute or search denied '" + file.getId() + "'");
        }
    }

    private void checkIsDirectory(File file) throws NotADirectoryException {
        if (file.isDirectory()) {
            throw new NotADirectoryException("File '" + file.getId() + "' is not a directory");
        }
    }

    private long getPermissionsBits(long mode) {
        return mode & Constants.PERMISSIONS_MASK;
    }

}
