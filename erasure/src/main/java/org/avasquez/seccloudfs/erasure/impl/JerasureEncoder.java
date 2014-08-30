package org.avasquez.seccloudfs.erasure.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.avasquez.seccloudfs.erasure.EncodingException;
import org.avasquez.seccloudfs.erasure.ErasureEncoder;
import org.avasquez.seccloudfs.erasure.Slices;
import org.avasquez.seccloudfs.erasure.utils.ByteBufferUtils;
import org.bridj.Pointer;
import org.springframework.beans.factory.annotation.Required;

/**
 * Implementation of {@link org.avasquez.seccloudfs.erasure.ErasureEncoder} that uses Jerasure.
 *
 * @author avasquez
 */
public class JerasureEncoder implements ErasureEncoder {

    private JerasureCodingMethod codingMethod;

    @Required
    public void setCodingMethod(JerasureCodingMethod codingMethod) {
        this.codingMethod = codingMethod;
    }

    @Override
    public Slices encode(ReadableByteChannel inputChannel, int size) throws EncodingException {
        int dataBufferSize = size;
        int k = codingMethod.getK();
        int m = codingMethod.getM();
        int w = codingMethod.getW();
        int packetSize = codingMethod.getPacketSize();
        int minSize = k * w * packetSize;
        int mod = size % minSize;

        // Calculate the data buffer size (must be a multiple of minSize)
        if (mod != 0) {
            if (size < minSize) {
                dataBufferSize = minSize;
            } else {
                dataBufferSize = size + minSize - mod;
            }
        }

        ByteBuffer dataBuffer = ByteBuffer.allocateDirect(dataBufferSize);
        dataBuffer.limit(size);

        // Read data into buffer
        try {
            inputChannel.read(dataBuffer);
        } catch (IOException e) {
            throw new EncodingException("Unable to read data from input channel", e);
        }

        dataBuffer.limit(dataBufferSize);

        // Pad the rest with zeroes
        padWithZeroes(dataBuffer);

        // Create pointers to data
        int sliceSize = dataBufferSize / k;
        ByteBuffer[] dataSlices = ByteBufferUtils.sliceBuffer(dataBuffer, k,  sliceSize);
        Pointer<Pointer<Byte>> dataPtrs = ByteBufferUtils.asPointers(dataSlices);

        // Allocate memory for coding and create pointers to coding
        ByteBuffer codingBuffer = ByteBuffer.allocateDirect(m * sliceSize);
        ByteBuffer[] codingSlices = ByteBufferUtils.sliceBuffer(codingBuffer, m, sliceSize);
        Pointer<Pointer<Byte>> codingPtrs = ByteBufferUtils.asPointers(codingSlices);

        // Do encoding
        codingMethod.encode(dataPtrs, codingPtrs, sliceSize);

        return new Slices(dataSlices, codingSlices);
    }

    private void padWithZeroes(ByteBuffer buffer) {
        while (buffer.position() < buffer.limit()) {
            buffer.put((byte) 0);
        }
    }

}
