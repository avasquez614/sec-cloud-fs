package org.avasquez.seccloudfs.filesystem.db.repos.impl;

import com.mongodb.MongoException;
import org.avasquez.seccloudfs.db.mongo.JongoRepository;
import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.filesystem.db.model.FileSystemInfo;
import org.avasquez.seccloudfs.filesystem.db.repos.FileSystemInfoRepository;
import org.jongo.Jongo;

/**
 * Created by alfonsovasquez on 16/02/14.
 */
public class JongoFileSystemInfoRepository extends JongoRepository<FileSystemInfo> implements FileSystemInfoRepository {

    public static final String FILESYSTEM_INFO_COLLECTION_NAME =  "fileSystemInfo";

    protected JongoFileSystemInfoRepository(Jongo jongo) {
        super(FILESYSTEM_INFO_COLLECTION_NAME, jongo);
    }

    @Override
    public Class<? extends FileSystemInfo> getPojoClass() {
        return FileSystemInfo.class;
    }

    @Override
    public FileSystemInfo getSingleton() throws DbException {
        try {
            return collection.findOne().as(FileSystemInfo.class);
        } catch (MongoException e) {
            throw new DbException("[" + collection.getName() + "] Get singleton " + FileSystemInfo.class.getName() +
                    " failed", e);
        }
    }

    @Override
    public void insert(FileSystemInfo pojo) throws DbException {
        if (count() == 0) {
            super.insert(pojo);
        } else {
            throw new DbException("[" + collection.getName() + "] An instance of " + FileSystemInfo.class.getName() +
                    " already exists in the DB");
        }
    }

}
