package org.avasquez.seccloudfs.amazon.utils;

import com.amazonaws.services.s3.transfer.TransferManager;

import org.avasquez.seccloudfs.utils.adapters.ClientFactory;

/**
 * {@link org.avasquez.seccloudfs.utils.adapters.ClientFactory} for creating {@link com.amazonaws.services.s3.transfer
 * .TransferManager}s.
 *
 * @author avasquez
 */
public class AmazonS3ClientFactory implements ClientFactory<TransferManager, AmazonCredentials> {

    @Override
    public TransferManager createClient(AmazonCredentials credentials) {
        final TransferManager tm = new TransferManager(credentials.getCredentials());

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                tm.shutdownNow();
            }

        });

        return tm;
    }

}
