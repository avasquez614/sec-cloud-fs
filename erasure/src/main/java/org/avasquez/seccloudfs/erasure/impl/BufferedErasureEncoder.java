package org.avasquez.seccloudfs.erasure.impl;

import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.avasquez.seccloudfs.erasure.EncodingException;
import org.avasquez.seccloudfs.erasure.ErasureEncoder;
import org.springframework.beans.factory.annotation.Required;

/**
 * {@link org.avasquez.seccloudfs.erasure.ErasureEncoder} decorator that does "buffered" encoding, that means,
 * just a certain amount of data is decoded at the same time. This means that the entire data doesn't need to be
 * loaded into memory for coding/decoding.
 *
 * @author avasquez
 */
public class BufferedErasureEncoder implements ErasureEncoder {

    private int bufferSize;
    private ErasureEncoder actualEncoder;

    @Required
    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    @Required
    public void setActualEncoder(ErasureEncoder actualEncoder) {
        this.actualEncoder = actualEncoder;
    }

    @Override
    public int getK() {
        return actualEncoder.getK();
    }

    @Override
    public int getM() {
        return actualEncoder.getM();
    }

    @Override
    public int encode(ReadableByteChannel input, int size, WritableByteChannel[] dataSlices,
                      WritableByteChannel[] codingSlices) throws EncodingException {
        if (size > bufferSize) {
            int encodedBytes = 0;
            int totalSliceSize = 0;

            while (encodedBytes < size) {
                int currentBufferSize = bufferSize;
                if ((encodedBytes + currentBufferSize) > size) {
                    currentBufferSize = size - encodedBytes;
                }

                totalSliceSize += actualEncoder.encode(input, currentBufferSize, dataSlices, codingSlices);
                encodedBytes += currentBufferSize;
            }

            return totalSliceSize;
        } else {
            return actualEncoder.encode(input, size, dataSlices, codingSlices);
        }
    }

}
