package org.avasquez.seccloudfs.filesystem.content;

import java.io.IOException;

import org.avasquez.seccloudfs.filesystem.util.FlushableByteChannel;

/**
 * Created by alfonsovasquez on 09/01/14.
 */
public interface Content {

    String getId();

    long getSize() throws IOException;

    FlushableByteChannel getByteChannel() throws IOException;

}
