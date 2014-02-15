package org.avasquez.seccloudfs.filesystem.content;

import java.io.IOException;

/**
 * Created by alfonsovasquez on 03/02/14.
 */
public interface CloudContent extends Content {

    boolean isDownloaded();

    boolean deleteDownload() throws IOException;

}
