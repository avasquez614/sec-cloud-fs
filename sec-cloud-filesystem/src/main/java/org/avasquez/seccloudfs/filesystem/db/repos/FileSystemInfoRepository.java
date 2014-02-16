package org.avasquez.seccloudfs.filesystem.db.repos;

import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.filesystem.db.model.FileSystemInfo;

/**
 * Created by alfonsovasquez on 16/02/14.
 */
public interface FileSystemInfoRepository {

    FileSystemInfo getSingleton() throws DbException;

    void insert(FileSystemInfo info) throws DbException;

}
