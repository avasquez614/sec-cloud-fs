package org.avasquez.seccloudfs.filesystem.fuse;

import net.fusejna.DirectoryFiller;
import net.fusejna.util.FuseFilesystemAdapterFull;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.avasquez.seccloudfs.filesystem.exception.*;
import org.avasquez.seccloudfs.filesystem.files.File;
import org.avasquez.seccloudfs.filesystem.files.FileStore;
import org.avasquez.seccloudfs.filesystem.files.User;
import org.avasquez.seccloudfs.filesystem.util.FlushableByteChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private static final Logger logger = Logger.getLogger(SecCloudFilesystem.class.getName());

    private FileStore fileStore;
    private FileHandleRegistry fileHandleRegistry;
    private long rootPermissions;
    private int rootUid;

    public SecCloudFilesystem() {
        rootPermissions = DEFAULT_ROOT_PERMISSIONS;
        rootUid = DEFAULT_ROOT_UID;
    }

    public void setFileStore(FileStore fileStore) {
        this.fileStore = fileStore;
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
            if (fileStore.getRoot() == null) {
                fileStore.createRoot(new User(getCurrentUid(), getCurrentGid()), rootPermissions);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "The root dir couldn't be retrieved or created", e);
        }
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

                fileStore.create(parent, getFilename(path), true, owner, permissions);

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

                checkDirectory(dir);
                checkWritePermission(dir.getParent());

                fileStore.delete(dir);

                return 0;
            }

        }, "rmdir");
    }

    @Override
    public int rename(final String path, final String newName) {
        return doWithErrorHandling(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                File file = resolveFile(path);

                checkWritePermission(file.getParent());

                File newParent = resolveParent(newName);

                checkDirectory(newParent);
                checkWritePermission(newParent);

                fileStore.move(file, newParent, newName);

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
                file.flushMetadata();

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
                file.flushMetadata();

                return 0;
            }

        }, "chown");
    }

    @Override
    public int truncate(final String path, final long offset) {
        return doWithErrorHandling(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                File file = resolveFile(path);

                checkWritePermission(file);

                try (FlushableByteChannel byteChannel = file.getByteChannel()) {
                    byteChannel.truncate(offset);
                }

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
                file.flushMetadata();

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

                FlushableByteChannel handle = file.getByteChannel();
                long handleId = fileHandleRegistry.addHandle(handle);

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

                FlushableByteChannel handle = getFileHandle(info.fh());
                handle.position(offset);

                int readBytes = handle.read(buffer);
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

                FlushableByteChannel handle = getFileHandle(info.fh());
                handle.position(offset);

                return handle.write(buffer);
            }

        }, "write");
    }

    private int doWithErrorHandling(Callable<Integer> method, String methodName) {
        try {
            return method.call();
        } catch (FileNotFoundException e) {
            logError(e, methodName);

            return -ENOENT();
        } catch (FileNotDirectoryException e) {
            logError(e, methodName);

            return -ENOTDIR();
        } catch (FileIsDirectoryException e) {
            logError(e, methodName);

            return -EISDIR();
        } catch (PermissionDeniedException e) {
            logError(e, methodName);

            return -EACCES();
        } catch (FileExistsException e) {
            logError(e, methodName);

            return -EEXIST();
        } catch (InvalidFileHandleException e) {
            logError(e, methodName);

            return -EBADF();
        }catch (Exception e) {
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

    private long secondsToMillis(long secs) {
        return TimeUnit.SECONDS.toMillis(secs);
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

        throw new FileNotFoundException("No file found at path " + path + " in dir '" + currentDir.getId() + "'");
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

    private void checkDirectory(File file) throws FileNotDirectoryException {
        if (!file.isDirectory()) {
            throw new FileNotDirectoryException("File '" + file.getId() + "' is not a directory");
        }
    }

    private void checkNotDirectory(File file) throws FileIsDirectoryException {
        if (file.isDirectory()) {
            throw new FileIsDirectoryException("File '" + file.getId() + "' is a directory");
        }
    }
    
    private FlushableByteChannel getFileHandle(long handleId) throws InvalidFileHandleException {
        FlushableByteChannel handle = fileHandleRegistry.getHandle(handleId);
        if (handle == null || !handle.isOpen()) {
            throw new InvalidFileHandleException("Non-existing or closed file handle " + handleId);
        }

        return handle;
    }

    private long getPermissionsBits(long mode) {
        return mode & Constants.PERMISSIONS_MASK;
    }

}
