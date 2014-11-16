package org.avasquez.seccloudfs.filesystem.util;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;

/**
 * A {@link java.nio.channels.SeekableByteChannel} tha can ble flushed to force any pending data to be written
 * to the storage device.
 *
 * @author avasquez
 */
public interface FlushableByteChannel extends SeekableByteChannel {

    void flush() throws IOException;

}
