package org.avasquez.seccloudfs.filesystem.files.impl;

import org.avasquez.seccloudfs.filesystem.content.Content;
import org.avasquez.seccloudfs.filesystem.files.File;

/**
 * Created by alfonsovasquez on 19/01/14.
 */
public interface ContentAwareFile extends File {

    Content getContent();

}
