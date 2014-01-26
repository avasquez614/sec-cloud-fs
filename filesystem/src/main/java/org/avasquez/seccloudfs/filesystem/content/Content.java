package org.avasquez.seccloudfs.filesystem.content;

import org.avasquez.seccloudfs.filesystem.util.FlushableByteChannel;

import java.io.IOException;

/**
 * Created by alfonsovasquez on 09/01/14.
 */
public interface Content {

    String getId();

    long getSize() throws IOException;

    FlushableByteChannel getByteChannel() throws IOException;

}
