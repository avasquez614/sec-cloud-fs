package org.avasquez.seccloudfs.filesystem.impl;

import org.avasquez.seccloudfs.filesystem.FileContent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;

/**
 * {@link org.avasquez.seccloudfs.filesystem.FileContent} decorator that logs write operations.
 *
 * @author avasquez
 */
public class LoggingFileContent implements FileContent {

    private FileContent underlyingContent;
    private String filePath;
    private WriteLog writeLog;

    public LoggingFileContent(FileContent underlyingContent, String filePath, WriteLog writeLog) {
        this.underlyingContent = underlyingContent;
        this.filePath = filePath;
        this.writeLog = writeLog;
    }

    @Override
    public long getPosition() throws IOException {
        return underlyingContent.getPosition();
    }

    @Override
    public void setPosition(long position) throws IOException {
        underlyingContent.setPosition(position);
    }

    @Override
    public int read(ByteBuffer dst, long position) throws IOException {
        return underlyingContent.read(dst, position);
    }

    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        writeLog.log(new WriteLogEntry(filePath, position, src.remaining(), new Date()));

        return underlyingContent.write(src, position);
    }

    @Override
    public void downloadAll() throws IOException {
        underlyingContent.downloadAll();
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return underlyingContent.read(dst);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        writeLog.log(new WriteLogEntry(filePath, getPosition(), src.remaining(), new Date()));

        return underlyingContent.write(src);
    }

    @Override
    public boolean isOpen() {
        return underlyingContent.isOpen();
    }

    @Override
    public void close() throws IOException {
        underlyingContent.close();
    }

}
