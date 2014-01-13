package org.avasquez.seccloudfs.filesystem.content;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;

/**
 * Created by alfonsovasquez on 09/01/14.
 */
public interface Content {

    String getId();

    long getSize();

    SeekableByteChannel getByteChannel() throws IOException;

    void copyFrom(Content srcContent) throws IOException;

}
