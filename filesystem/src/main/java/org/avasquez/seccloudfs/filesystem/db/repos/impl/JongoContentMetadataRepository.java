package org.avasquez.seccloudfs.filesystem.db.repos.impl;

import org.avasquez.seccloudfs.db.mongo.JongoRepository;
import org.avasquez.seccloudfs.filesystem.db.model.ContentMetadata;
import org.avasquez.seccloudfs.filesystem.db.repos.ContentMetadataRepository;
import org.jongo.Jongo;

/**
 * Created by alfonsovasquez on 02/02/14.
 */
public class JongoContentMetadataRepository extends JongoRepository<ContentMetadata> implements ContentMetadataRepository {

    public static final String CONTENT_METADATA_COLLECTION_NAME = "contentMetadata";

    public JongoContentMetadataRepository(Jongo jongo) {
        super(CONTENT_METADATA_COLLECTION_NAME, jongo);
    }

    @Override
    public Class<? extends ContentMetadata> getPojoClass() {
        return ContentMetadata.class;
    }

}
