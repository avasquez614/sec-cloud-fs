package org.avasquez.seccloudfs.filesystem.files.impl;

import org.avasquez.seccloudfs.filesystem.content.Content;
import org.avasquez.seccloudfs.filesystem.files.File;

/**
 * Created by alfonsovasquez on 01/02/14.
 */
public interface FileObject extends File {

    String getId();

    DirectoryEntries getEntries();

    Content getContent();

}
