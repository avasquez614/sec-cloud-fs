package org.avasquez.seccloudfs.gdrive.utils;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialRefreshListener;
import com.google.api.client.auth.oauth2.TokenErrorResponse;
import com.google.api.client.auth.oauth2.TokenResponse;

import java.io.IOException;

import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.gdrive.db.model.GoogleDriveCredential;
import org.avasquez.seccloudfs.gdrive.db.repos.GoogleDriveCredentialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link com.google.api.client.auth.oauth2.CredentialRefreshListener} that updates the stored
 * {@link org.avasquez.seccloudfs.gdrive.db.model.GoogleDriveCredential} on refresh success and deletes it on
 * refresh error.
 *
 * @author avasquez
 */
public class RepositoryCredentialRefreshListener implements CredentialRefreshListener {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryCredentialRefreshListener.class);

    private String credentialId;
    private GoogleDriveCredentialRepository credentialRepository;

    public RepositoryCredentialRefreshListener(final String credentialId,
                                               final GoogleDriveCredentialRepository credentialRepository) {
        this.credentialId = credentialId;
        this.credentialRepository = credentialRepository;
    }

    @Override
    public void onTokenResponse(final Credential credential, final TokenResponse tokenResponse) throws IOException {
        logger.debug("Credential '" + credentialId + "' refreshed");

        GoogleDriveCredential storedCredential = new GoogleDriveCredential();
        storedCredential.setId(credentialId);
        storedCredential.setAccessToken(credential.getAccessToken());
        storedCredential.setRefreshToken(credential.getRefreshToken());
        storedCredential.setExpirationTime(credential.getExpirationTimeMilliseconds());

        try {
            credentialRepository.save(storedCredential);
        } catch (DbException e) {
            throw new IOException("Unable to save credential '" + credentialId + "'");
        }
    }

    @Override
    public void onTokenErrorResponse(final Credential credential,
                                     final TokenErrorResponse tokenErrorResponse) throws IOException {
        logger.error("An error occurred on refreshing credential '{}': error = {}, errorDescription = {}, " +
            "errorUri = {}", tokenErrorResponse.getError(), tokenErrorResponse.getErrorDescription(),
            tokenErrorResponse.getErrorUri());

        try {
            credentialRepository.delete(credentialId);
        } catch (DbException e) {
            throw new IOException("Unable to delete credential '" + credentialId + "'", e);
        }
    }

}
