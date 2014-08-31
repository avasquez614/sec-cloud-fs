package org.avasquez.seccloudfs.dropbox.db.model;

import org.jongo.marshall.jackson.oid.Id;
import org.jongo.marshall.jackson.oid.ObjectId;

/**
 * Represents Dropbox credential info that's stored in the DB.
 *
 * @author avasquez
 */
public class DropboxCredential {

    @Id
    @ObjectId
    private String id;
    private String accessToken;

    /**
     * Private no-arg constructor, for use by frameworks like Jongo/Jackson.
     */
    private DropboxCredential() {
    }

    public DropboxCredential(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

}
