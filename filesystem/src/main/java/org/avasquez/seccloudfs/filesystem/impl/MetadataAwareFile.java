package org.avasquez.seccloudfs.filesystem.impl;

import org.avasquez.seccloudfs.filesystem.File;
import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;

/**
 * A {@link org.avasquez.seccloudfs.filesystem.File} that can return an instance of
 * {@link org.avasquez.seccloudfs.filesystem.db.model.FileMetadata}.
 *
 * @author avasquez
 */
public interface MetadataAwareFile extends File {

    /**
     * Returns the file's metadata.
     */
    FileMetadata getMetadata();

}
