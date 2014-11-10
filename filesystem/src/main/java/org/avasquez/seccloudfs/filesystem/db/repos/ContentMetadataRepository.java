package org.avasquez.seccloudfs.filesystem.db.repos;

import org.avasquez.seccloudfs.db.Repository;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.filesystem.db.model.ContentMetadata;

/**
 * Created by alfonsovasquez on 12/01/14.
 */
public interface ContentMetadataRepository extends Repository<ContentMetadata> {

    Iterable<ContentMetadata> findMarkedAsDelete() throws DbException;

}
