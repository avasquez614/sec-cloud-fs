package org.avasquez.seccloudfs.gdrive.db.repos.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.avasquez.seccloudfs.db.impl.FileRepository;
import org.avasquez.seccloudfs.gdrive.db.model.GoogleDriveCredentials;
import org.avasquez.seccloudfs.gdrive.db.repos.GoogleDriveCredentialsRepository;

import java.io.File;

/**
 * {@link org.avasquez.seccloudfs.db.impl.FileRepository} for {@link org.avasquez.seccloudfs.gdrive.db.model
 * .GoogleDriveCredentials}.
 *
 * @author avasquez
 */
public class FileGoogleDriveCredentialsRepository extends FileRepository<GoogleDriveCredentials>
        implements GoogleDriveCredentialsRepository {

    public static final String CREDENTIALS_FILENAME_EXT = ".gcred";

    public FileGoogleDriveCredentialsRepository(File credentialsDirectory) {
        super(credentialsDirectory);
    }

    public FileGoogleDriveCredentialsRepository(File credentialsDirectory, ObjectMapper jsonMapper) {
        super(credentialsDirectory, jsonMapper);
    }

    @Override
    public Class<GoogleDriveCredentials> getPojoClass() {
        return GoogleDriveCredentials.class;
    }

    @Override
    public String getPojoFilenameExtension() {
        return CREDENTIALS_FILENAME_EXT;
    }

    @Override
    public String getPojoId(GoogleDriveCredentials pojo) {
        return pojo.getId();
    }

    @Override
    public void setPojoId(String id, GoogleDriveCredentials pojo) {
        pojo.setId(id);
    }

}
