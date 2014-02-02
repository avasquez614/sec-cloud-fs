package org.avasquez.seccloudfs.filesystem.content;

import org.avasquez.seccloudfs.filesystem.util.SyncAwareByteChannel;

import java.io.IOException;

/**
 * Created by alfonsovasquez on 09/01/14.
 */
public interface Content {

    String getId();

    long getSize() throws IOException;

    SyncAwareByteChannel getByteChannel() throws IOException;

}
