package org.avasquez.seccloudfs.filesystem.content;

import java.io.IOException;

/**
 * Represents content stored in the cloud.
 *
 * @author avasquez
 */
public interface CloudContent extends Content {

    boolean isDownloaded();

    boolean deleteDownload() throws IOException;

    void forceUpload() throws IOException;

}
