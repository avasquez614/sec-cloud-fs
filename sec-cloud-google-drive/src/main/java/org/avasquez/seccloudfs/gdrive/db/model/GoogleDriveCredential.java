package org.avasquez.seccloudfs.gdrive.db.model;

import org.jongo.marshall.jackson.oid.Id;
import org.jongo.marshall.jackson.oid.ObjectId;

/**
 * Represents Google Drive credential info that's stored in the DB.
 *
 * @author avasquez
 */
public class GoogleDriveCredential {

    @Id
    @ObjectId
    private String id;
    private String userId;
    private String accessToken;
    private String refreshToken;
    private Long expirationTime;

    /**
     * Private no-arg constructor, for use by frameworks like Jongo/Jackson.
     */
    private GoogleDriveCredential() {
    }

    public GoogleDriveCredential(final String userId, final String accessToken, final String refreshToken,
                                 final Long expirationTime) {
        this.userId = userId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expirationTime = expirationTime;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public Long getExpirationTime() {
        return expirationTime;
    }

}
