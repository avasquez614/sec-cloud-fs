package org.avasquez.seccloudfs.dropbox.db.repos.impl;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

import org.avasquez.seccloudfs.db.impl.FileRepository;
import org.avasquez.seccloudfs.dropbox.db.model.DropboxCredentials;
import org.avasquez.seccloudfs.dropbox.db.repos.DropboxCredentialsRepository;

/**
 * {@link org.avasquez.seccloudfs.db.impl.FileRepository} for {@link org.avasquez.seccloudfs.dropbox.db.model
 * .DropboxCredentials}.
 *
 * @author avasquez
 */
public class FileDropboxCredentialsRepository extends FileRepository<DropboxCredentials>
        implements DropboxCredentialsRepository {

    public static final String CREDENTIALS_FILENAME_EXT = ".dcred";

    public FileDropboxCredentialsRepository(File credentialsDirectory) {
        super(credentialsDirectory);
    }

    public FileDropboxCredentialsRepository(File credentialsDirectory, ObjectMapper jsonMapper) {
        super(credentialsDirectory, jsonMapper);
    }

    @Override
    public Class<DropboxCredentials> getPojoClass() {
        return DropboxCredentials.class;
    }

    @Override
    public String getPojoFilenameExtension() {
        return CREDENTIALS_FILENAME_EXT;
    }

    @Override
    public String getPojoId(DropboxCredentials pojo) {
        return pojo.getId();
    }

    @Override
    public void setPojoId(String id, DropboxCredentials pojo) {
        pojo.setId(id);
    }

}
