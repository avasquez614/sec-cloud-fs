package org.avasquez.seccloudfs.amazon.utils;

import com.amazonaws.services.s3.transfer.TransferManager;

import javax.annotation.PreDestroy;

import org.avasquez.seccloudfs.utils.adapters.ClientFactory;

/**
 * {@link org.avasquez.seccloudfs.utils.adapters.ClientFactory} for creating {@link com.amazonaws.services.s3.transfer
 * .TransferManager}s.
 *
 * @author avasquez
 */
public class AmazonS3ClientFactory implements ClientFactory<TransferManager, AmazonCredentials> {

    private TransferManager tm;

    @PreDestroy
    public void destroy() {
        tm.shutdownNow();
    }

    @Override
    public TransferManager createClient(AmazonCredentials credentials) {
        tm = new TransferManager(credentials.getCredentials());

        return tm;
    }

}
