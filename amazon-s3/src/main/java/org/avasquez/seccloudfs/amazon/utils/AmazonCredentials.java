package org.avasquez.seccloudfs.amazon.utils;

import com.amazonaws.auth.AWSCredentials;

/**
 * Simple wrapper to {@link com.amazonaws.auth.AWSCredentials} that adds an account ID to identify the credentials.
 *
 * @author avasquez
 */
public class AmazonCredentials {

    private String accountId;
    private AWSCredentials credentials;

    public AmazonCredentials(String accountId, AWSCredentials credentials) {
        this.accountId = accountId;
        this.credentials = credentials;
    }

    public String getAccountId() {
        return accountId;
    }

    public AWSCredentials getCredentials() {
        return credentials;
    }

}
