package org.avasquez.seccloudfs.filesystem.db.dao;

import org.avasquez.seccloudfs.filesystem.db.model.ContentMetadata;

/**
 * Created by alfonsovasquez on 12/01/14.
 */
public interface ContentMetadataDao {

    ContentMetadata find(String id);

    void insert(ContentMetadata metadata);

    void save(ContentMetadata metadata);

    void delete(String id);

}
