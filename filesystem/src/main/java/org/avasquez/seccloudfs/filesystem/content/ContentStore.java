package org.avasquez.seccloudfs.filesystem.content;

import java.io.IOException;

/**
 * Created by alfonsovasquez on 09/01/14.
 */
public interface ContentStore {

    Content find(String id) throws IOException;

    Content create() throws IOException;

    void delete(Content content) throws IOException;

    long getTotalSpace() throws IOException;

    long getAvailableSpace() throws IOException;

}
