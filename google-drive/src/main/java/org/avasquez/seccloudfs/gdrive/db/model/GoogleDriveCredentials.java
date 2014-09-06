package org.avasquez.seccloudfs.gdrive.db.model;

import com.google.api.client.auth.oauth2.Credential;

import org.bson.types.ObjectId;
import org.jongo.marshall.jackson.oid.Id;

/**
 * Represents Google Drive credentials info that's stored in the DB.
 *
 * @author avasquez
 */
public class GoogleDriveCredentials {

    @Id
    private String id;
    private String username;
    private String accessToken;
    private String refreshToken;
    private Long expirationTime;

    /**
     * Generates an ID for the credential.
     */
    public static String generateId() {
        return ObjectId.get().toString();
    }

    public GoogleDriveCredentials() {
    }

    public GoogleDriveCredentials(String id, Credential credential) {
        this.id = id;
        this.accessToken = credential.getAccessToken();
        this.refreshToken = credential.getRefreshToken();
        this.expirationTime = credential.getExpirationTimeMilliseconds();
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(final String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(final String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(final Long expirationTime) {
        this.expirationTime = expirationTime;
    }

}
