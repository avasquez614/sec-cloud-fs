package org.avasquez.seccloudfs.erasure.impl;

import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.avasquez.seccloudfs.erasure.DecodingException;
import org.avasquez.seccloudfs.erasure.ErasureDecoder;
import org.springframework.beans.factory.annotation.Required;

/**
 * {@link org.avasquez.seccloudfs.erasure.ErasureDecoder} decorator that does "buffered" decoding, that means,
 * just a certain amount of data is decoded at the same time. This means that the entire data doesn't need to be
 * loaded into memory for coding/decoding.
 *
 * @author avasquez
 */
public class BufferedErasureDecoder implements ErasureDecoder {

    private int bufferSize;
    private ErasureDecoder actualDecoder;

    @Required
    public void setBufferSize(final int bufferSize) {
        this.bufferSize = bufferSize;
    }

    @Required
    public void setActualDecoder(final ErasureDecoder actualDecoder) {
        this.actualDecoder = actualDecoder;
    }

    @Override
    public int getK() {
        return actualDecoder.getK();
    }

    @Override
    public int getM() {
        return actualDecoder.getM();
    }

    @Override
    public void decode(int originalSize, ReadableByteChannel[] dataSlices, ReadableByteChannel[] codingSlices,
                       WritableByteChannel output) throws DecodingException {
        if (originalSize > bufferSize) {
            int decodedBytes = 0;

            while (decodedBytes < originalSize) {
                int currentBufferSize = bufferSize;
                if ((decodedBytes + currentBufferSize) > originalSize) {
                    currentBufferSize = originalSize - decodedBytes;
                }

                actualDecoder.decode(currentBufferSize, dataSlices, codingSlices, output);

                decodedBytes += currentBufferSize;
            }
        } else {
            actualDecoder.decode(originalSize, dataSlices, codingSlices, output);
        }
    }

}
