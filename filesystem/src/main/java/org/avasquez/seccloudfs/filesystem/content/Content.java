package org.avasquez.seccloudfs.filesystem.content;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;

/**
 * Created by alfonsovasquez on 09/01/14.
 */
public interface Content {

    String getId();

    long getSize() throws IOException;

    SeekableByteChannel getByteChannel() throws IOException;

    void copyTo(Content target) throws IOException;

}
