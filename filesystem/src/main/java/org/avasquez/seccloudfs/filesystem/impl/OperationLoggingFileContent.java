package org.avasquez.seccloudfs.filesystem.impl;

import org.avasquez.seccloudfs.filesystem.FileContent;
import org.avasquez.seccloudfs.filesystem.db.dao.FileOperationDao;
import org.avasquez.seccloudfs.filesystem.db.model.TruncateOperation;
import org.avasquez.seccloudfs.filesystem.db.model.WriteOperation;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;

/**
 * {@link org.avasquez.seccloudfs.filesystem.FileContent} decorator that logs write and truncate operations.
 *
 * @author avasquez
 */
public class OperationLoggingFileContent implements FileContent {

    private FileContent underlyingContent;
    private String filePath;
    private FileOperationDao operationDao;

    public OperationLoggingFileContent(FileContent underlyingContent, String filePath, FileOperationDao operationDao) {
        this.underlyingContent = underlyingContent;
        this.filePath = filePath;
        this.operationDao = operationDao;
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
        WriteOperation operation = new WriteOperation();
        operation.setPath(filePath);
        operation.setPosition(position);
        operation.setLength(src.remaining());
        operation.setBeginTime(new Date());

        operationDao.insert(operation);

        int written = underlyingContent.write(src, position);

        operation.setLength(written);
        operation.setEndTime(new Date());

        operationDao.update(operation);

        return written;
    }

    @Override
    public void copyTo(FileContent target) throws IOException {
        underlyingContent.copyTo(target);
    }

    @Override
    public void truncate(long size) throws IOException {
        TruncateOperation operation = new TruncateOperation();
        operation.setPath(filePath);
        operation.setSize(size);
        operation.setBeginTime(new Date());

        operationDao.insert(operation);

        underlyingContent.truncate(size);

        operation.setEndTime(new Date());

        operationDao.update(operation);
    }

    @Override
    public void delete() throws IOException {
        underlyingContent.delete();
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return underlyingContent.read(dst);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        WriteOperation operation = new WriteOperation();
        operation.setPath(filePath);
        operation.setPosition(underlyingContent.getPosition());
        operation.setLength(src.remaining());
        operation.setBeginTime(new Date());

        operationDao.insert(operation);

        int written = underlyingContent.write(src);

        operation.setLength(written);
        operation.setEndTime(new Date());

        operationDao.update(operation);

        return written;
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
