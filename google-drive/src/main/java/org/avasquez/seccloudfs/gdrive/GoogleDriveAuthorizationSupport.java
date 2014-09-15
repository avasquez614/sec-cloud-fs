package org.avasquez.seccloudfs.gdrive;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.annotation.PostConstruct;

import org.avasquez.seccloudfs.gdrive.db.model.GoogleDriveCredentials;
import org.avasquez.seccloudfs.gdrive.db.repos.GoogleDriveCredentialsRepository;
import org.springframework.beans.factory.annotation.Required;

/**
 * Helper class that handles OAuth2 authorization with Google to get credentials for Google Drive. Credentials are
 * then stored in the DB.
 *
 * @author avasquez
 */
public class GoogleDriveAuthorizationSupport {

    private static final String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";
    private static final List<String> SCOPES = Arrays.asList(
        "https://www.googleapis.com/auth/drive.file",
        "https://www.googleapis.com/auth/userinfo.email",
        "https://www.googleapis.com/auth/userinfo.profile");

    private String clientId;
    private String clientSecret;
    private GoogleDriveCredentialsRepository credentialsRepository;

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
    public void setCredentialsRepository(GoogleDriveCredentialsRepository credentialsRepository) {
        this.credentialsRepository = credentialsRepository;
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
    public GoogleDriveCredentials exchangeCode(String authorizationCode) throws IOException {
        TokenResponse response = flow.newTokenRequest(authorizationCode).setRedirectUri(REDIRECT_URI).execute();

        return createCredentials(response);
    }

    private void createFlow() {
        HttpTransport transport = new NetHttpTransport();
        JsonFactory jsonFactory = new JacksonFactory();
        flow = new GoogleAuthorizationCodeFlow.Builder(transport, jsonFactory, clientId, clientSecret, SCOPES)
            .setAccessType("offline")
            .setApprovalPrompt("force")
            .build();
    }

    private GoogleDriveCredentials createCredentials(TokenResponse tokenResponse) {
        HttpTransport transport = new NetHttpTransport();
        JsonFactory jsonFactory = new JacksonFactory();

        Credential credentials = new GoogleCredential.Builder()
            .setTransport(transport)
            .setJsonFactory(jsonFactory)
            .setClientSecrets(clientId, clientSecret)
            .build()
            .setFromTokenResponse(tokenResponse);

        GoogleDriveCredentials storedCredentials = new GoogleDriveCredentials();
        storedCredentials.setAccessToken(credentials.getAccessToken());
        storedCredentials.setRefreshToken(credentials.getRefreshToken());
        storedCredentials.setExpirationTime(credentials.getExpirationTimeMilliseconds());

        return storedCredentials;
    }

}
