package org.avasquez.seccloudfs.filesystem.fuse;

import net.fusejna.DirectoryFiller;
import net.fusejna.util.FuseFilesystemAdapterFull;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.avasquez.seccloudfs.filesystem.exception.*;
import org.avasquez.seccloudfs.filesystem.files.File;
import org.avasquez.seccloudfs.filesystem.files.Filesystem;
import org.avasquez.seccloudfs.filesystem.files.User;
import org.avasquez.seccloudfs.filesystem.util.SyncAwareByteChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static net.fusejna.ErrorCodes.*;
import static net.fusejna.StructFuseFileInfo.FileInfoWrapper;
import static net.fusejna.StructStat.StatWrapper;
import static net.fusejna.StructTimeBuffer.TimeBufferWrapper;
import static net.fusejna.types.TypeMode.ModeWrapper;
import static net.fusejna.types.TypeMode.NodeType;

/**
 * Created by alfonsovasquez on 25/01/14.
 */
public class SecCloudFilesystem extends FuseFilesystemAdapterFull {

    public static final long DEFAULT_ROOT_PERMISSIONS = (7L << 6) | (5L << 3) | 5L;

    public static final int DEFAULT_ROOT_UID =  0;

    /**
     * Separator for file path components.
     */
    public static final String PATH_SEPARATOR = "/";

    private static final Logger logger = LoggerFactory.getLogger(SecCloudFilesystem.class);

    private Filesystem filesystem;
    private FileHandleRegistry fileHandleRegistry;
    private long rootPermissions;
    private int rootUid;

    public SecCloudFilesystem() {
        rootPermissions = DEFAULT_ROOT_PERMISSIONS;
        rootUid = DEFAULT_ROOT_UID;
    }

    public void setFilesystem(Filesystem filesystem) {
        this.filesystem = filesystem;
    }

    public void setFileHandleRegistry(FileHandleRegistry fileHandleRegistry) {
        this.fileHandleRegistry = fileHandleRegistry;
    }

    public void setRootPermissions(long rootPermissions) {
        this.rootPermissions = rootPermissions;
    }

    public void setRootUid(int rootUid) {
        this.rootUid = rootUid;
    }

    @Override
    public void init() {
        try {
            if (filesystem.getRoot() == null) {
                filesystem.createRoot(new User(getCurrentUid(), getCurrentGid()), rootPermissions);
            }
        } catch (IOException e) {
            logger.error("The root dir couldn't be retrieved or created", e);
        }
    }

    @Override
    public void destroy() {
        fileHandleRegistry.destroyAll();
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

                checkDirectory(dir);
                checkReadPermission(dir);

                filler.add(dir.getChildren());

                updateLastAccessTime(dir, false);

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

                checkDirectory(parent);
                checkWritePermission(parent);

                User owner = new User(getCurrentUid(), getCurrentGid());
                long permissions = getPermissionsBits(mode.mode());

                parent.createFile(getFilename(path), true, owner, permissions);
                updateLastModifiedTime(parent, true);

                return 0;
            }

        }, "mkdir");
    }

    @Override
    public int rmdir(final String path) {
        return doWithErrorHandling(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                File parent = resolveParent(path);
                String name = getFilename(path);
                File dir = getChild(parent, name);

                checkDirectory(dir);
                checkWritePermission(parent);

                parent.delete(name);
                updateLastModifiedTime(parent, true);

                return 0;
            }

        }, "rmdir");
    }

    @Override
    public int rename(final String path, final String newName) {
        return doWithErrorHandling(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                File parent = resolveParent(path);

                checkWritePermission(parent);

                File newParent = resolveParent(newName);

                checkDirectory(newParent);
                checkWritePermission(newParent);

                parent.moveFileTo(getFilename(path), newParent, getFilename(newName));

                updateLastModifiedTime(parent, true);
                updateLastModifiedTime(newParent, true);

                return 0;
            }

        }, "rename");
    }

    @Override
    public int chmod(final String path, final ModeWrapper mode) {
        return doWithErrorHandling(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                File file = resolveFile(path);

                checkWritePermission(file);

                long permissions = getPermissionsBits(mode.mode());

                file.setPermissions(permissions);
                updateLastChangeTime(file, true);

                return 0;
            }

        }, "chmod");
    }

    @Override
    public int chown(final String path, final long uid, final long gid) {
        return doWithErrorHandling(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                if (getCurrentUid() != rootUid) {
                    throw new PermissionDeniedException("Only root can call chown");
                }

                File file = resolveFile(path);

                file.setOwner(new User(uid, gid));
                updateLastChangeTime(file, true);

                return 0;
            }

        }, "chown");
    }

    @Override
    public int create(final String path, final ModeWrapper mode, final FileInfoWrapper info) {
        return doWithErrorHandling(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                File parent = resolveParent(path);

                checkDirectory(parent);
                checkWritePermission(parent);

                String name = getFilename(path);
                File file = null;

                if (!parent.hasChild(name)) {
                    synchronized (parent) {
                        if (!parent.hasChild(name)) {
                            User owner = new User(getCurrentUid(), getCurrentGid());
                            long permissions = getPermissionsBits(mode.mode());

                            file = parent.createFile(name, false, owner, permissions);
                            updateLastModifiedTime(parent, true);
                        }
                    }
                }

                if (file == null) {
                    file = parent.getChild(name);
                }

                SyncAwareByteChannel handle = file.getByteChannel();
                long handleId = fileHandleRegistry.add(handle);

                info.fh(handleId);

                return 0;
            }

        }, "create");
    }

    @Override
    public int unlink(final String path) {
        return doWithErrorHandling(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                File parent = resolveParent(path);
                String name = getFilename(path);
                File file = getChild(parent, name);

                checkNotDirectory(file);
                checkWritePermission(parent);

                parent.delete(name);
                updateLastModifiedTime(parent, true);

                return 0;
            }

        }, "unlink");
    }

    @Override
    public int truncate(final String path, final long offset) {
        return doWithErrorHandling(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                File file = resolveFile(path);

                checkWritePermission(file);

                try (SyncAwareByteChannel handle = file.getByteChannel()) {
                    handle.truncate(offset);
                }

                updateLastModifiedTime(file, true);

                return 0;
            }

        }, "truncate");
    }

    @Override
    public int utimens(final String path, final TimeBufferWrapper timeBuffer) {
        return doWithErrorHandling(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                File file = resolveFile(path);

                checkWritePermission(file);

                file.setLastAccessTime(new Date(secondsToMillis(timeBuffer.ac_sec())));
                file.setLastModifiedTime(new Date(secondsToMillis(timeBuffer.mod_sec())));
                file.syncMetadata();

                return 0;
            }

        }, "utimens");
    }

    @Override
    public int open(final String path, final FileInfoWrapper info) {
        return doWithErrorHandling(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                File file = resolveFile(path);

                checkNotDirectory(file);

                SyncAwareByteChannel handle = file.getByteChannel();
                long handleId = fileHandleRegistry.add(handle);

                info.fh(handleId);

                return 0;
            }

        }, "open");
    }

    @Override
    public int read(final String path, final ByteBuffer buffer, final long size, final long offset,
                    final FileInfoWrapper info) {
        return doWithErrorHandling(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                File file = resolveFile(path);

                checkReadPermission(file);

                buffer.limit(buffer.position() + (int) size);

                SyncAwareByteChannel handle = getFileHandle(info.fh());
                handle.position(offset);

                int readBytes = handle.read(buffer);

                updateLastAccessTime(file, false);

                if (readBytes > 0) {
                    return readBytes;
                } else {
                    return 0;
                }
            }

        }, "read");
    }

    @Override
    public int write(final String path, final ByteBuffer buffer, final long size, final long offset,
                     final FileInfoWrapper info) {
        return doWithErrorHandling(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                File file = resolveFile(path);

                checkWritePermission(file);

                buffer.limit(buffer.position() + (int) size);

                SyncAwareByteChannel handle = getFileHandle(info.fh());
                handle.position(offset);

                int writtenBytes = handle.write(buffer);

                updateLastModifiedTime(file, false);

                return writtenBytes;
            }

        }, "write");
    }

    @Override
    public int release(final String path, final FileInfoWrapper info) {
        return doWithErrorHandling(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                File file = resolveFile(path);
                file.syncMetadata();

                fileHandleRegistry.destroy(info.fh());

                return 0;
            }

        }, "release");
    }

    @Override
    public int releasedir(final String path, final FileInfoWrapper info) {
        return doWithErrorHandling(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                File dir = resolveFile(path);
                dir.syncMetadata();

                return 0;
            }

        }, "releasedir");
    }

    @Override
    public int fsync(final String path, final int datasync, final FileInfoWrapper info) {
        return doWithErrorHandling(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                File file = resolveFile(path);
                SyncAwareByteChannel handle = getFileHandle(info.fh());

                if (datasync == 0) {
                    file.syncMetadata();
                }

                handle.sync();

                return 0;
            }

        }, "fsync");
    }

    private int doWithErrorHandling(Callable<Integer> method, String methodName) {
        try {
            return method.call();
        } catch (FileNotFoundException e) {
            logError(e, methodName, true);

            return -ENOENT();
        } catch (FileNotDirectoryException e) {
            logError(e, methodName, true);

            return -ENOTDIR();
        } catch (FileIsDirectoryException e) {
            logError(e, methodName, true);

            return -EISDIR();
        } catch (PermissionDeniedException e) {
            logError(e, methodName, true);

            return -EACCES();
        } catch (FileExistsException e) {
            logError(e, methodName, true);

            return -EEXIST();
        } catch (InvalidFileHandleException e) {
            logError(e, methodName, true);

            return -EBADF();
        }catch (Exception e) {
            logError(e, methodName, false);

            return -EIO();
        }
    }

    private void logError(Throwable ex, String methodName, boolean debug) {
        if (debug) {
            if (logger.isDebugEnabled()) {
                logger.debug("Method '" + methodName + "' failed", ex);
            }
        } else {
            logger.error("Method '" + methodName + "' failed", ex);
        }
    }

    private long millisToSeconds(long millis) {
        return TimeUnit.MILLISECONDS.toSeconds(millis);
    }

    private long secondsToMillis(long secs) {
        return TimeUnit.SECONDS.toMillis(secs);
    }

    private File resolveFile(String path) throws PermissionDeniedException, IOException {
        return resolveFile(filesystem.getRoot(), path);
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
            return getChild(currentDir, path);
        } else {
            File nextDir = getChild(currentDir, path.substring(0, indexOfSep));

            return resolveFile(nextDir, path.substring(indexOfSep));
        }
    }

    private File resolveParent(String path) throws PermissionDeniedException, IOException {
        String parentPath = StringUtils.stripEnd(FilenameUtils.getPath(path), PATH_SEPARATOR);

        return resolveFile(parentPath);
    }

    private String getFilename(String path) {
        return StringUtils.stripEnd(FilenameUtils.getName(path), PATH_SEPARATOR);
    }

    private File getChild(File parent, String name) throws IOException {
        File file = parent.getChild(name);
        if (file == null) {
            throw new FileNotFoundException("No file found with name " + name + " in dir '" + parent + "'");
        }

        return file;
    }

    private int getCurrentUid() {
        return getFuseContextUid().intValue();
    }

    private int getCurrentGid() {
        return getFuseContextGid().intValue();
    }

    private boolean hasPermission(File file, int permission) {
        if (getCurrentUid() == rootUid) {
            return true;
        } else if (getCurrentUid() == file.getOwner().getUid()) {
            return (file.getPermissions() & (permission << 6)) > 0;
        } else if (getCurrentGid() == file.getOwner().getGid()) {
            return (file.getPermissions() & (permission << 3)) > 0;
        } else {
            return (file.getPermissions() & permission) > 0;
        }
    }

    private void checkReadPermission(File file) throws PermissionDeniedException {
        if (!hasPermission(file, Constants.R_OK)) {
            throw new PermissionDeniedException("Read denied for '" + file + "'");
        }
    }

    private void checkWritePermission(File file) throws PermissionDeniedException {
        if (!hasPermission(file, Constants.W_OK)) {
            throw new PermissionDeniedException("Write denied for '" + file + "'");
        }
    }

    private void checkExecutePermission(File file) throws PermissionDeniedException {
        if (!hasPermission(file, Constants.X_OK)) {
            throw new PermissionDeniedException("Execute or search denied for '" + file + "'");
        }
    }

    private void checkDirectory(File file) throws FileNotDirectoryException {
        if (!file.isDirectory()) {
            throw new FileNotDirectoryException("File '" + file + "' is not a directory");
        }
    }

    private void checkNotDirectory(File file) throws FileIsDirectoryException {
        if (file.isDirectory()) {
            throw new FileIsDirectoryException("File '" + file + "' is a directory");
        }
    }
    
    private SyncAwareByteChannel getFileHandle(long handleId) throws InvalidFileHandleException {
        SyncAwareByteChannel handle = fileHandleRegistry.get(handleId);
        if (handle == null || !handle.isOpen()) {
            throw new InvalidFileHandleException("Non-existing or closed file handle " + handleId);
        }

        return handle;
    }

    private long getPermissionsBits(long mode) {
        return mode & Constants.PERMISSIONS_MASK;
    }

    private void updateLastAccessTime(File file, boolean sync) throws IOException {
        file.setLastAccessTime(new Date());
        if (sync) {
            file.syncMetadata();
        }
    }

    private void updateLastChangeTime(File file, boolean sync) throws IOException {
        file.setLastChangeTime(new Date());
        if (sync) {
            file.syncMetadata();
        }
    }

    private void updateLastModifiedTime(File file, boolean sync) throws IOException {
        Date now = new Date();

        file.setLastChangeTime(now);
        file.setLastModifiedTime(now);
        if (sync) {
            file.syncMetadata();
        }
    }

}
