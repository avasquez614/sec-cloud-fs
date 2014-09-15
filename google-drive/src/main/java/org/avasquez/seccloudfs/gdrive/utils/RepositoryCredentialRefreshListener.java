package org.avasquez.seccloudfs.gdrive.utils;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialRefreshListener;
import com.google.api.client.auth.oauth2.TokenErrorResponse;
import com.google.api.client.auth.oauth2.TokenResponse;

import java.io.IOException;

import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.gdrive.db.model.GoogleDriveCredentials;
import org.avasquez.seccloudfs.gdrive.db.repos.GoogleDriveCredentialsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link com.google.api.client.auth.oauth2.CredentialRefreshListener} that updates the stored
 * {@link org.avasquez.seccloudfs.gdrive.db.model.GoogleDriveCredentials} on refresh success and deletes it on
 * refresh error.
 *
 * @author avasquez
 */
public class RepositoryCredentialRefreshListener implements CredentialRefreshListener {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryCredentialRefreshListener.class);

    private String credentialsId;
    private GoogleDriveCredentialsRepository credentialsRepository;

    public RepositoryCredentialRefreshListener(String credentialsId,
                                               GoogleDriveCredentialsRepository credentialsRepository) {
        this.credentialsId = credentialsId;
        this.credentialsRepository = credentialsRepository;
    }

    @Override
    public void onTokenResponse(Credential credentials, TokenResponse tokenResponse) throws IOException {
        logger.debug("Credentials '{}' refreshed", credentialsId);

        GoogleDriveCredentials storedCredentials;
        try {
            storedCredentials = credentialsRepository.find(credentialsId);
        } catch (DbException e) {
            throw new IOException("Unable to retrieve credentials '" + credentialsId + "' from DB", e);
        }

        storedCredentials.setAccessToken(credentials.getAccessToken());
        storedCredentials.setRefreshToken(credentials.getRefreshToken());
        storedCredentials.setExpirationTime(credentials.getExpirationTimeMilliseconds());

        try {
            credentialsRepository.save(storedCredentials);
        } catch (DbException e) {
            throw new IOException("Unable to save credentials '" + credentialsId + "' in DB", e);
        }

        logger.debug("Refreshed credentials '{}' saved in DB", credentialsId);
    }

    @Override
    public void onTokenErrorResponse(Credential credentials,
                                     TokenErrorResponse tokenErrorResponse) throws IOException {
        logger.error("An error occurred on refreshing credentials '{}': error = {}, errorDescription = {}, " +
            "errorUri = {}", tokenErrorResponse.getError(), tokenErrorResponse.getErrorDescription(),
            tokenErrorResponse.getErrorUri());

        try {
            credentialsRepository.delete(credentialsId);
        } catch (DbException e) {
            throw new IOException("Unable to delete credentials '" + credentialsId + "' from DB", e);
        }

        logger.debug("Deleted credentials '{}' from DB", credentialsId);
    }

}
