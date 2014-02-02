package org.avasquez.seccloudfs.filesystem.db.dao.impl;

import org.avasquez.seccloudfs.db.mongo.JongoDao;
import org.avasquez.seccloudfs.filesystem.db.dao.ContentMetadataDao;
import org.avasquez.seccloudfs.filesystem.db.model.ContentMetadata;
import org.jongo.Jongo;

/**
 * Created by alfonsovasquez on 02/02/14.
 */
public class JongoContentMetadataDao extends JongoDao<ContentMetadata> implements ContentMetadataDao {

    public static final String CONTENT_METADATA_COLLECTION_NAME = "contentMetadata";

    public JongoContentMetadataDao(Jongo jongo) {
        super(CONTENT_METADATA_COLLECTION_NAME, jongo);
    }

    @Override
    public Class<? extends ContentMetadata> getPojoClass() {
        return ContentMetadata.class;
    }

}
