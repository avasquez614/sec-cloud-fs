package org.avasquez.seccloudfs.dropbox.utils;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxRequestConfig;

import org.avasquez.seccloudfs.dropbox.db.model.DropboxCredentials;
import org.avasquez.seccloudfs.utils.adapters.ClientFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * {@link org.avasquez.seccloudfs.utils.adapters.ClientFactory} for creating {@link com.dropbox.core.DbxClient}s.
 *
 * @author avasquez
 */
public class DropboxClientFactory implements ClientFactory<DbxClient, DropboxCredentials> {

    private DbxRequestConfig requestConfig;

    @Required
    public void setRequestConfig(final DbxRequestConfig requestConfig) {
        this.requestConfig = requestConfig;
    }

    @Override
    public DbxClient createClient(DropboxCredentials credentials) {
        return new DbxClient(requestConfig, credentials.getAccessToken());
    }

}
