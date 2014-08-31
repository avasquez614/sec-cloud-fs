package org.avasquez.seccloudfs.gdrive;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialRefreshListener;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.DriveScopes;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.annotation.PostConstruct;

import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.gdrive.db.model.GoogleDriveCredential;
import org.avasquez.seccloudfs.gdrive.db.repos.GoogleDriveCredentialRepository;
import org.avasquez.seccloudfs.gdrive.utils.RepositoryCredentialRefreshListener;
import org.springframework.beans.factory.annotation.Required;

/**
 * Helper class that handles OAuth2 authorization with Google to get credentials for Google Drive. Credentials are
 * then stored in the DB.
 *
 * @author avasquez
 */
public class GoogleDriveAuthorizationSupport {

    private static final String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";
    private static final List<String> SCOPES = Arrays.asList(DriveScopes.DRIVE);

    private String clientId;
    private String clientSecret;
    private GoogleDriveCredentialRepository credentialRepository;

    private GoogleAuthorizationCodeFlow flow;

    @Required
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Required
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    @Required
    public void setCredentialRepository(GoogleDriveCredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
    }

    @PostConstruct
    public void init() {
        createFlow();
    }

    /**
     * Returns the authorization URL the user must go to in order to authorize access of the Google Drive to
     * the Sec Cloud FS app.
     */
    public String getAuthorizationUrl() {
        return flow.newAuthorizationUrl().setRedirectUri(REDIRECT_URI).build();
    }

    /**
     * Exchanges an authorization code for OAuth 2.0 credential. The credentials are then stored in the DB.
     *
     * @param authorizationCode authorization code to exchange for OAuth 2.0 credentials
     *
     * @return the OAuth 2.0 credential
     */
    public GoogleDriveCredential exchangeCode(String authorizationCode) throws IOException {
        TokenResponse response = flow.newTokenRequest(authorizationCode).setRedirectUri(REDIRECT_URI).execute();
        String credentialId = GoogleDriveCredential.generateId();
        Credential credential = createCredential(credentialId, response);
        GoogleDriveCredential storedCredential = new GoogleDriveCredential(credentialId, credential);

        try {
            credentialRepository.insert(storedCredential);
        } catch (DbException e) {
            throw new IOException("Unable to insert credential '" + credentialId + "'");
        }

        return storedCredential;
    }

    private void createFlow() {
        HttpTransport transport = new NetHttpTransport();
        JsonFactory jsonFactory = new JacksonFactory();
        flow = new GoogleAuthorizationCodeFlow.Builder(transport, jsonFactory, clientId, clientSecret, SCOPES)
            .setAccessType("offline")
            .setApprovalPrompt("force")
            .build();
    }

    private Credential createCredential(String id, TokenResponse tokenResponse) {
        HttpTransport transport = new NetHttpTransport();
        JsonFactory jsonFactory = new JacksonFactory();
        CredentialRefreshListener refreshListener = new RepositoryCredentialRefreshListener(id, credentialRepository);

        return new GoogleCredential.Builder()
            .setTransport(transport)
            .setJsonFactory(jsonFactory)
            .setClientSecrets(clientId, clientSecret)
            .addRefreshListener(refreshListener)
            .build()
            .setFromTokenResponse(tokenResponse);
    }

}
