package org.avasquez.seccloudfs.filesystem.files.impl;

import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;
import org.avasquez.seccloudfs.filesystem.files.File;

/**
 * Created by alfonsovasquez on 19/01/14.
 */
public interface MetadataAwareFile extends File {

    FileMetadata getMetadata();

}
