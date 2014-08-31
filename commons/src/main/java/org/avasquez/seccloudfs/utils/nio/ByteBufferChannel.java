package org.avasquez.seccloudfs.utils.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

/**
 * Adapts a {@link java.nio.ByteBuffer} as a {@link java.nio.channels.SeekableByteChannel}.
 *
 * @author avasquez
 */
public class ByteBufferChannel implements SeekableByteChannel {

    private ByteBuffer buffer;

    public ByteBufferChannel(ByteBuffer buffer) {
        buffer.clear();

        this.buffer = buffer;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        int bytesToRead = buffer.remaining();

        dst.put(buffer);

        return bytesToRead;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        int bytesToWrite = src.remaining();

        buffer.put(src);

        return bytesToWrite;
    }

    @Override
    public long position() throws IOException {
        return buffer.position();
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        buffer.position((int) newPosition);

        return this;
    }

    @Override
    public long size() throws IOException {
        return buffer.capacity();
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        return null;
    }

    @Override
    public boolean isOpen() {
        // Always open
        return true;
    }

    @Override
    public void close() throws IOException {
        // Do nothing
    }

}
