package org.avasquez.seccloudfs.filesystem.content.impl;

import org.avasquez.seccloudfs.filesystem.content.Content;
import org.avasquez.seccloudfs.filesystem.db.dao.ContentMetadataDao;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by alfonsovasquez on 12/01/14.
 */
public class CloudContentStore extends AbstractCachedContentStore {

    private ContentMetadataDao contentMetadataDao;
    private String cacheContentDir;

    public void setContentMetadataDao(ContentMetadataDao contentMetadataDao) {
        this.contentMetadataDao = contentMetadataDao;
    }

    public void setCacheContentDir(String cacheContentDir) {
        this.cacheContentDir = cacheContentDir;
    }

    @Override
    protected Content doFind(String id) {
        Path path = Paths.get(cacheContentDir, id);

        CloudContent content = new CloudContent();
    }

    @Override
    protected Content doCreate() {
        return null;
    }

    @Override
    protected void doDelete(String id) {

    }

}
