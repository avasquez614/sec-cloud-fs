package org.avasquez.seccloudfs.gdrive.db.model;

/**
 * Represents Google Drive credentials info that's stored in the DB.
 *
 * @author avasquez
 */
public class GoogleDriveCredentials {

    //@Id
    //@ObjectId
    private String id;
    private String accountId;
    private String accessToken;
    private String refreshToken;
    private Long expirationTime;

    public GoogleDriveCredentials() {
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(final String accountId) {
        this.accountId = accountId;
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
