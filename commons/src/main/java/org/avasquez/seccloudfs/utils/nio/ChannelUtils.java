package org.avasquez.seccloudfs.utils.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Utility methods for NIO channels.
 *
 * @author avasquez
 */
public class ChannelUtils {

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 8;

    private ChannelUtils() {
    }

    /**
     * Copy the specified data from the readable channel to the writable channel.
     *
     * @param in    the channel to read the data from
     * @param out   the channel to write the data to
     *
     * @return the number of bytes copied
     */
    public static int copy(ReadableByteChannel in, WritableByteChannel out) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
        int count = 0;
        int n;

        while ((n = in.read(buffer)) != -1) {
            buffer.flip(); // Prepare the buffer to be drained

            while (buffer.hasRemaining()) {
                out.write(buffer);
            }

            buffer.clear(); // Empty buffer to get ready for filling

            count += n;
        }

        return count;
    }

}
