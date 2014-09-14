package org.avasquez.seccloudfs.utils.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * {@link java.nio.channels.ReadableByteChannel} decorator that notifies listeners whenever data has been read from
 * the underlying channel.
 *
 * @author avasquez
 */
public class NotifyingReadableByteChannel implements ReadableByteChannel {

    private ReadableByteChannel underlyingChannel;
    private Iterable<ChannelListener> listeners;

    public NotifyingReadableByteChannel(ReadableByteChannel underlyingChannel, Iterable<ChannelListener> listeners) {
        this.underlyingChannel = underlyingChannel;
        this.listeners = listeners;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        int bytesRead = underlyingChannel.read(dst);

        for (ChannelListener listener : listeners) {
            listener.onUpdate(bytesRead);
        }

        return bytesRead;
    }

    @Override
    public boolean isOpen() {
        return underlyingChannel.isOpen();
    }

    @Override
    public void close() throws IOException {
        underlyingChannel.close();
    }

}
