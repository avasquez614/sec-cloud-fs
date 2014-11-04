package org.avasquez.seccloudfs.filesystem.fuse;

import net.fusejna.DirectoryFiller;
import net.fusejna.util.FuseFilesystemAdapterFull;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.avasquez.seccloudfs.filesystem.exception.*;
import org.avasquez.seccloudfs.filesystem.files.File;
import org.avasquez.seccloudfs.filesystem.files.FileSystem;
import org.avasquez.seccloudfs.filesystem.files.User;
import org.avasquez.seccloudfs.filesystem.util.FlushableByteChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static net.fusejna.ErrorCodes.*;
import static net.fusejna.StructFuseFileInfo.FileInfoWrapper;
import static net.fusejna.StructStat.StatWrapper;
import static net.fusejna.StructStatvfs.StatvfsWrapper;
import static net.fusejna.StructTimeBuffer.TimeBufferWrapper;
import static net.fusejna.types.TypeMode.ModeWrapper;
import static net.fusejna.types.TypeMode.NodeType;

/**
 * Created by alfonsovasquez on 25/01/14.
 */
public class SecCloudFS extends FuseFilesystemAdapterFull {

    private static final Logger logger = LoggerFactory.getLogger(SecCloudFS.class);

    public static final String APP_CONTEXT_LOCATION = "classpath:filesystem-context.xml";

    private String[] options;
    private FileSystem fileSystem;
    private FileHandleRegistry fileHandleRegistry;
    private long rootPermissions;
    private int rootUid;
    private long blockSize;

    public static void main(String... args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: seccloudfs <mountpoint>");
            System.exit(1);
        }

        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(APP_CONTEXT_LOCATION);
        context.registerShutdownHook();

        context.getBean(SecCloudFS.class).log(true).mount(args[0]);
    }

    @Override
    protected String[] getOptions() {
        return options;
    }

    @Required
    public void setOptions(String[] options) {
        if (ArrayUtils.isNotEmpty(options)) {
            this.options = options;
        }
    }

    @Required
    public void setFileSystem(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    @Required
    public void setFileHandleRegistry(FileHandleRegistry fileHandleRegistry) {
        this.fileHandleRegistry = fileHandleRegistry;
    }

    @Required
    public void setRootPermissions(String octalPermissions) {
        this.rootPermissions = Long.valueOf(octalPermissions, 8);
    }

    @Required
    public void setRootUid(int rootUid) {
        this.rootUid = rootUid;
    }

    @Required
    public void setBlockSize(long blockSize) {
        this.blockSize = blockSize;
    }

    @Override
    public void init() {
        try {
            if (fileSystem.getRoot() == null) {
                File root = fileSystem.createRoot(new User(getMountUid(), getMountGid()), rootPermissions);

                logger.info("Filesystem root folder created: {}", root);
            }
        } catch (IOException e) {
            logger.error("The root dir couldn't be retrieved or created", e);
        }
    }

    @Override
    public void destroy() {
        logger.info("Destroying all remaining file handles");

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
                long uid = file.getOwner().getUid();
                long gid = file.getOwner().getGid();
                long size = file.getSize();
                Date ctime = file.getLastChangeTime();
                Date atime = file.getLastAccessTime();
                Date mtime = file.getLastModifiedTime();

                stat.mode(mode);
                stat.uid(uid);
                stat.gid(gid);
                stat.size(size);
                stat.ctime(millisToSeconds(ctime.getTime()));
                stat.atime(millisToSeconds(atime.getTime()));
                stat.mtime(millisToSeconds(mtime.getTime()));

                if (logger.isDebugEnabled()) {
                    logger.debug("{} requested attributes of file at {}: mode={},uid={},gid={}," +
                            "size={},ctime={},atime={},mtime={}", getCurrentUser(), path, mode, uid,
                            gid, size, ctime, atime, mtime);
                }

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
                    if (logger.isDebugEnabled()) {
                        logger.debug("Access allowed to {} to file at {}", getCurrentUser(), path);
                    }

                    return 0;
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Access denied to {} to file at {}", getCurrentUser(), path);
                    }

                    return -EACCES();
                }
            }

        }, "access");
    }

    @Override
    public int opendir(final String path, final FileInfoWrapper info) {
        return doWithErrorHandling(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                File dir = resolveFile(path);

                checkDirectory(dir);

                updateLastAccessTime(dir, false);

                return 0;
            }

        }, "opendir");
    }

    @Override
    public int readdir(final String path, final DirectoryFiller filler) {
        return doWithErrorHandling(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                File dir = resolveFile(path);

                checkDirectory(dir);
                checkReadPermission(dir);

                String[] children = dir.getChildren();

                filler.add(children);

                if (logger.isDebugEnabled()) {
                    logger.debug("{} read dir at {}: children={}", getCurrentUser(), path, Arrays.toString(children));
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

                checkDirectory(parent);
                checkWritePermission(parent);

                User owner = new User(getCurrentUid(), getCurrentGid());
                long permissions = getPermissionsBits(mode.mode());

                parent.createFile(getFilename(path), true, owner, permissions);
                updateLastModifiedTime(parent, true);

                if (logger.isDebugEnabled()) {
                    logger.debug("{} created dir at {}", getCurrentUser(), path);
                }

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

                if (logger.isDebugEnabled()) {
                    logger.debug("{} removed dir at {}", getCurrentUser(), path);
                }

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

                if (logger.isDebugEnabled()) {
                    logger.debug("{} moved file from {} to {}", getCurrentUser(), path, newName);
                }

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

                if (logger.isDebugEnabled()) {
                    String permStr = toPermissionsString(permissions);

                    logger.debug("{} changed permissions of file at {} to {}", getCurrentUser(), path, permStr);
                }

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
                User user = new User(uid, gid);

                file.setOwner(user);
                updateLastChangeTime(file, true);

                if (logger.isDebugEnabled()) {
                    logger.debug("{} changed owner of file at {} to {}", getCurrentUser(), path, user);
                }

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

                            if (logger.isDebugEnabled()) {
                                logger.debug("{} created file at {}", getCurrentUser(), path);
                            }
                        }
                    }
                }

                if (file == null) {
                    file = parent.getChild(name);
                }

                FlushableByteChannel handle = file.getByteChannel();
                long handleId = fileHandleRegistry.register(handle);

                info.fh(handleId);

                if (logger.isDebugEnabled()) {
                    logger.debug("{} opened file at {}: fileHandle={}", getCurrentUser(), path, handleId);
                }

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

                if (logger.isDebugEnabled()) {
                    logger.debug("{} removed file at {}", getCurrentUser(), path);
                }

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

                try (FlushableByteChannel handle = file.getByteChannel()) {
                    handle.truncate(offset);
                }

                updateLastModifiedTime(file, true);

                if (logger.isDebugEnabled()) {
                    logger.debug("{} truncated file at {} to {}", getCurrentUser(), path, offset);
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

                Date lastAccessTime = new Date(secondsToMillis(timeBuffer.ac_sec()));
                Date lastModifiedTime = new Date(secondsToMillis(timeBuffer.mod_sec()));

                file.setLastAccessTime(lastAccessTime);
                file.setLastChangeTime(lastModifiedTime);
                file.setLastModifiedTime(lastModifiedTime);
                file.syncMetadata();

                if (logger.isDebugEnabled()) {
                    logger.debug("{} updated last access and last modified time of file at {}: atime={},mtime={}",
                            getCurrentUser(), path, file.getLastAccessTime(), file.getLastModifiedTime());
                }

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
                long handleId = fileHandleRegistry.register(handle);

                info.fh(handleId);

                updateLastAccessTime(file, false);

                if (logger.isDebugEnabled()) {
                    logger.debug("{} opened file at {}: fileHandle={}", getCurrentUser(), path, handleId);
                }

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

                if (logger.isDebugEnabled()) {
                    logger.debug("{} read {} bytes from file at {}", getCurrentUser(), readBytes, path);
                }

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

                int writtenBytes = handle.write(buffer);

                updateLastModifiedTime(file, false);

                if (logger.isDebugEnabled()) {
                    logger.debug("{} wrote {} bytes to file at {}", getCurrentUser(), writtenBytes, path);
                }

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

                if (logger.isDebugEnabled()) {
                    logger.debug("{} released file at {}: fileHandle={}", getCurrentUser(), path, info.fh());
                }

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
                FlushableByteChannel handle = getFileHandle(info.fh());

                if (datasync == 0) {
                    file.syncMetadata();

                    if (logger.isDebugEnabled()) {
                        logger.debug("{} flushed metadata of file at {} to storage", getCurrentUser(), path);
                    }
                }

                handle.flush();

                if (logger.isDebugEnabled()) {
                    logger.debug("{} flushed data of file at {} to storage", getCurrentUser(), path);
                }

                return 0;
            }

        }, "fsync");
    }

    @Override
    public int statfs(final String path, final StatvfsWrapper stats) {
        return doWithErrorHandling(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                long totalSpace = fileSystem.getTotalSpace();
                long availableSpace = fileSystem.getAvailableSpace();
                long totalBlocks = totalSpace / blockSize;
                long availableBlocks = availableSpace / blockSize;
                long freeFileNodes = Integer.MAX_VALUE - fileSystem.getTotalFiles();

                stats.bsize(blockSize);
                stats.frsize(blockSize);
                stats.blocks(totalBlocks);
                stats.bfree(availableBlocks);
                stats.bavail(availableBlocks);
                stats.files(Integer.MAX_VALUE);
                stats.ffree(freeFileNodes);
                stats.favail(freeFileNodes);

                if (logger.isDebugEnabled()) {
                    logger.debug("{} requested filesystem status: bsize={},frsize={},blocks={},bfree={}," +
                            "bavail={},files={},ffree={},favail={}", getCurrentUser(), blockSize, blockSize,
                            totalBlocks, availableBlocks, availableBlocks, Integer.MAX_VALUE, freeFileNodes,
                            freeFileNodes);
                }

                return 0;
            }

        }, "statfs");
    }

    private int doWithErrorHandling(Callable<Integer> method, String methodName) {
        try {
            return method.call();
        } catch (FileNotFoundException e) {
            logError(e.getMessage(), methodName);

            return -ENOENT();
        } catch (NotDirectoryException e) {
            logError(e.getMessage(), methodName);

            return -ENOTDIR();
        } catch (IsDirectoryException e) {
            logError(e.getMessage(), methodName);

            return -EISDIR();
        } catch (PermissionDeniedException e) {
            logError(e.getMessage(), methodName);

            return -EACCES();
        } catch (FileExistsException e) {
            logError(e.getMessage(), methodName);

            return -EEXIST();
        } catch (InvalidFileHandleException e) {
            logError(e.getMessage(), methodName);

            return -EBADF();
        }catch (Exception e) {
            logError(e, methodName);

            return -EIO();
        }
    }

    private void logError(String message, String methodName) {
        logger.debug("Method '{}' failed: {}", methodName, message);
    }    

    private void logError(Throwable ex, String methodName) {
        logger.error("Method '" + methodName + "' failed", ex);
    }

    private long millisToSeconds(long millis) {
        return TimeUnit.MILLISECONDS.toSeconds(millis);
    }

    private long secondsToMillis(long secs) {
        return TimeUnit.SECONDS.toMillis(secs);
    }

    private File resolveFile(String path) throws PermissionDeniedException, IOException {
        return resolveFile(fileSystem.getRoot(), path);
    }

    private File resolveFile(File currentDir, String path) throws PermissionDeniedException, IOException {
        path = StringUtils.strip(path, Constants.PATH_SEPARATOR);

        if (path.isEmpty()) {
            return currentDir;
        }

        // Execute permission means the user can search the directory
        checkExecutePermission(currentDir);

        int indexOfSep = path.indexOf(Constants.PATH_SEPARATOR);
        if (indexOfSep < 0) {
            return getChild(currentDir, path);
        } else {
            File nextDir = getChild(currentDir, path.substring(0, indexOfSep));

            return resolveFile(nextDir, path.substring(indexOfSep));
        }
    }

    private File resolveParent(String path) throws PermissionDeniedException, IOException {
        String parentPath = StringUtils.stripEnd(FilenameUtils.getPath(path), Constants.PATH_SEPARATOR);

        return resolveFile(parentPath);
    }

    private String getFilename(String path) {
        return StringUtils.stripEnd(FilenameUtils.getName(path), Constants.PATH_SEPARATOR);
    }

    private File getChild(File parent, String name) throws IOException {
        File file = parent.getChild(name);
        if (file == null) {
            throw new FileNotFoundException("No file found with name " + name + " in dir '" + parent + "'");
        }

        return file;
    }

    private User getCurrentUser() {
        return new User(getCurrentUid(), getCurrentGid());
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

    private void checkDirectory(File file) throws NotDirectoryException {
        if (!file.isDirectory()) {
            throw new NotDirectoryException("File '" + file + "' is not a directory");
        }
    }

    private void checkNotDirectory(File file) throws IsDirectoryException {
        if (file.isDirectory()) {
            throw new IsDirectoryException("File '" + file + "' is a directory");
        }
    }
    
    private FlushableByteChannel getFileHandle(long handleId) throws InvalidFileHandleException {
        FlushableByteChannel handle = fileHandleRegistry.get(handleId);
        if (handle == null) {
            throw new InvalidFileHandleException("Non-existing file handle " + handleId);
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

    private String toPermissionsString(long permissions) {
        StringBuilder str = new StringBuilder();

        for (int i = 8; i >= 0; i--) {
            long mask = 1 << i;

            if ((permissions & mask) == 1) {
                if ((i + 1) % 3 == 0) {
                    str.append('r');
                } else if ((i + 1) % 3 == 2) {
                    str.append('w');
                } else {
                    str.append('x');
                }
            } else {
                str.append('-');
            }
        }

        return str.toString();
    }

}
