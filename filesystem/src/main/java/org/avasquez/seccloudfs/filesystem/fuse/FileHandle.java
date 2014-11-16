package org.avasquez.seccloudfs.filesystem.fuse;

import org.avasquez.seccloudfs.filesystem.files.File;
import org.avasquez.seccloudfs.filesystem.util.FlushableByteChannel;

/**
 * Represents a file handle, created on file open and used in later calls to read, write and release. Contains the
 * current file object and the byte channel to write to the content.
 *
 * @author avasquez
 */
public class FileHandle {

    private File file;
    private FlushableByteChannel channel;

    public FileHandle(File file, FlushableByteChannel channel) {
        this.file = file;
        this.channel = channel;
    }

    public File getFile() {
        return file;
    }

    public FlushableByteChannel getChannel() {
        return channel;
    }

}
