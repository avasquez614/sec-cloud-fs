package org.avasquez.seccloudfs.amazon;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;

import org.avasquez.seccloudfs.cloud.impl.MaxSizeAwareCloudStore;

/**
 * Amazon S3 implementation of {@link org.avasquez.seccloudfs.cloud.CloudStore}.
 *
 * @author avasquez
 */
public class AmazonS3CloudStore extends MaxSizeAwareCloudStore {

    private String name;
    private AmazonS3 client;
    private String bucketName;

    @Override
    public String getName() {
        return null;
    }

    @Override
    protected Object getMetadata(final String id) throws IOException {
        return null;
    }

    @Override
    protected long doUpload(final Object metadata, final SeekableByteChannel src, final long length) throws IOException {
        return 0;
    }

    @Override
    protected long doDownload(final Object metadata, final SeekableByteChannel target) throws IOException {
        return 0;
    }

    @Override
    protected void doDelete(final Object metadata) throws IOException {

    }

    @Override
    protected long getDataSize(final Object metadata) throws IOException {
        return 0;
    }

    @Override
    protected long calculateCurrentSize() throws IOException {
        try {
            ObjectListing objectListing = client.listObjects(new ListObjectsRequest().withBucketName(bucketName));
            long total = getTotalSizeOfObjects(objectListing);

            while (objectListing.isTruncated()) {
                objectListing = client.listNextBatchOfObjects(objectListing);
                total += getTotalSizeOfObjects(objectListing);
            }

            return total;
        } catch (AmazonClientException e) {
            throw new IOException("Unable to calculate size of bucket '" + bucketName + "' in store " + name, e);
        }
    }

    private long getTotalSizeOfObjects(ObjectListing objectListing) {
        long total = 0;

        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
            total += objectSummary.getSize();
        }

        return total;
    }

}
