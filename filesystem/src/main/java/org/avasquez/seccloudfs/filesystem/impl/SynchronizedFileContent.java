package org.avasquez.seccloudfs.filesystem.impl;

import org.avasquez.seccloudfs.filesystem.FileContent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;

/**
 * Decorator for a {@link org.avasquez.seccloudfs.filesystem.FileContent} that synchronizes read/write calls with a
 * {@link java.util.concurrent.locks.Lock}.
 *
 * <p>
 * Locks should be used with {@link org.avasquez.seccloudfs.filesystem.FileContent}s because although each one of
 * them probably uses a different instance of {@link java.nio.channels.FileChannel}, we don't want different threads
 * to read/write at the same regions of the file at the same time.
 * </p>
 *
 * @author avasquez
 */
public class SynchronizedFileContent implements FileContent {

    private FileContent underlyingContent;
    private Lock lock;

    public SynchronizedFileContent(FileContent underlyingContent, Lock lock) {
        this.underlyingContent = underlyingContent;
        this.lock = lock;
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
    public int read(ByteBuffer dst) throws IOException {
        lock.lock();
        try {
            return underlyingContent.read(dst);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        lock.lock();
        try {
            return underlyingContent.write(src);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int read(ByteBuffer dst, long position) throws IOException {
        lock.lock();
        try {
            return underlyingContent.read(dst, position);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        lock.lock();
        try {
            return underlyingContent.read(src, position);
        } finally {
            lock.unlock();
        }
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
