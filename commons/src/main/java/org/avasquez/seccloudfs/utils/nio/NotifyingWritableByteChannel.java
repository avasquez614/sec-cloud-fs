package org.avasquez.seccloudfs.utils.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * {@link java.nio.channels.WritableByteChannel} decorator that notifies listeners whenever data has been written to
 * the underlying channel.
 *
 * @author avasquez
 */
public class NotifyingWritableByteChannel implements WritableByteChannel {

    private WritableByteChannel underlyingChannel;
    private Iterable<ChannelListener> listeners;

    public NotifyingWritableByteChannel(WritableByteChannel underlyingChannel, Iterable<ChannelListener> listeners) {
        this.underlyingChannel = underlyingChannel;
        this.listeners = listeners;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        int bytesWrote = underlyingChannel.write(src);

        for (ChannelListener listener : listeners) {
            listener.onUpdate(bytesWrote);
        }

        return bytesWrote;
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
