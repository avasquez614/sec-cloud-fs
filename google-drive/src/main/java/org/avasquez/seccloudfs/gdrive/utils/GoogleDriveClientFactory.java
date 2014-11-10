package org.avasquez.seccloudfs.gdrive.utils;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialRefreshListener;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;

import java.io.IOException;

import org.avasquez.seccloudfs.gdrive.db.model.GoogleDriveCredentials;
import org.avasquez.seccloudfs.gdrive.db.repos.GoogleDriveCredentialsRepository;
import org.avasquez.seccloudfs.utils.adapters.ClientFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * {@link org.avasquez.seccloudfs.utils.adapters.ClientFactory} for {@link com.google.api.services.drive.Drive}s.
 *
 * @author avasquez
 */
public class GoogleDriveClientFactory implements ClientFactory<Drive, GoogleDriveCredentials> {

    private static final int REQUEST_TIMEOUT_MILLIS = 60 * 1000;

    private String clientId;
    private String clientSecret;
    private String applicationName;
    private GoogleDriveCredentialsRepository credentialsRepository;

    @Required
    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    @Required
    public void setClientSecret(final String clientSecret) {
        this.clientSecret = clientSecret;
    }

    @Required
    public void setApplicationName(final String applicationName) {
        this.applicationName = applicationName;
    }

    @Required
    public void setCredentialsRepository(final GoogleDriveCredentialsRepository credentialsRepository) {
        this.credentialsRepository = credentialsRepository;
    }

    @Override
    public Drive createClient(GoogleDriveCredentials credentials) {
        HttpTransport transport = new NetHttpTransport();
        JsonFactory jsonFactory = new JacksonFactory();
        CredentialRefreshListener refreshListener = new RepositoryCredentialRefreshListener(
            credentials,
            credentialsRepository);

        final Credential cred = new GoogleCredential.Builder()
            .setTransport(transport)
            .setJsonFactory(jsonFactory)
            .setClientSecrets(clientId, clientSecret)
            .addRefreshListener(refreshListener)
            .build()
            .setAccessToken(credentials.getAccessToken())
            .setRefreshToken(credentials.getRefreshToken())
            .setExpirationTimeMilliseconds(credentials.getExpirationTime());

        return new Drive.Builder(transport, jsonFactory, new HttpRequestInitializer() {

                @Override
                public void initialize(HttpRequest request) throws IOException {
                    cred.initialize(request);

                    request.setConnectTimeout(REQUEST_TIMEOUT_MILLIS);
                    request.setReadTimeout(REQUEST_TIMEOUT_MILLIS);
                }

            })
            .setApplicationName(applicationName)
            .build();
    }

}
