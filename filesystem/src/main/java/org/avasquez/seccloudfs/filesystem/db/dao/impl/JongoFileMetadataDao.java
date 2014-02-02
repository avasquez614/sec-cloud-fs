package org.avasquez.seccloudfs.filesystem.db.dao.impl;

import org.avasquez.seccloudfs.db.mongo.JongoDao;
import org.avasquez.seccloudfs.filesystem.db.dao.FileMetadataDao;
import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;
import org.jongo.Jongo;

/**
 * Created by alfonsovasquez on 02/02/14.
 */
public class JongoFileMetadataDao extends JongoDao<FileMetadata> implements FileMetadataDao {

    public static final String FILE_METADATA_COLLECTION_NAME = "fileMetadata";

    public JongoFileMetadataDao(Jongo jongo) {
        super(FILE_METADATA_COLLECTION_NAME, jongo);
    }

    @Override
    public Class<? extends FileMetadata> getPojoClass() {
        return FileMetadata.class;
    }

}
