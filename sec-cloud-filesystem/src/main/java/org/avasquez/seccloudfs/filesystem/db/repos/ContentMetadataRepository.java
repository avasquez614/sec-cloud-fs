package org.avasquez.seccloudfs.filesystem.db.repos;

import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.filesystem.db.model.ContentMetadata;

/**
 * Created by alfonsovasquez on 12/01/14.
 */
public interface ContentMetadataRepository {

    ContentMetadata find(String id) throws DbException;

    void insert(ContentMetadata metadata) throws DbException;

    void save(ContentMetadata metadata) throws DbException;

    void delete(String id) throws DbException;

}
