package org.avasquez.seccloudfs.filesystem.util;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;

/**
 * Created by alfonsovasquez on 25/01/14.
 */
public interface FlushableByteChannel extends SeekableByteChannel {

    void flush() throws IOException;

}
