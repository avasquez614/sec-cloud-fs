package org.avasquez.seccloudfs.utils.nio;

/**
 * Listens for read/write progress of a channel.
 *
 * @author avasquez
 * @see NotifyingReadableByteChannel
 */
public interface ChannelListener {

    /**
     * Method called whenever new data has been read or written.
     *
     * @param bytes the number of bytes that have been read/written
     */
    void onUpdate(int bytes);

}
