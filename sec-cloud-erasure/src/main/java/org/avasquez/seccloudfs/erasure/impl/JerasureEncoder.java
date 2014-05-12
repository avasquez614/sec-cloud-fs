package org.avasquez.seccloudfs.erasure.impl;

import org.avasquez.seccloudfs.erasure.EncodingException;
import org.avasquez.seccloudfs.erasure.ErasureEncoder;
import org.avasquez.seccloudfs.erasure.Fragments;
import org.bridj.Pointer;
import org.springframework.beans.factory.annotation.Required;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

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
    public Fragments encode(ReadableByteChannel inputChannel, int size) throws EncodingException {
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
                dataBufferSize = size + mod;
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
        Pointer<Byte> dataBufferPtr = Pointer.pointerToBytes(dataBuffer);
        Pointer<Pointer<Byte>> dataPtrs = Pointer.allocatePointers(Byte.class, k);

        int fragmentSize = dataBufferSize / k;

        for (int i = 0; i < k; i++) {
            dataPtrs.set(i, dataBufferPtr.offset(i * fragmentSize));
        }

        Pointer<Pointer<Byte>> codingPtrs = Pointer.allocateBytes(m, fragmentSize);

        // Do encoding
        codingMethod.encode(dataPtrs, codingPtrs, fragmentSize);

        // Create data and coding fragment arrays
        ByteBuffer[] dataFragments = asFragmentArray(dataPtrs, k, fragmentSize);
        ByteBuffer[] codingFragments = asFragmentArray(codingPtrs, m, fragmentSize);

        return new Fragments(dataFragments, codingFragments);
    }

    private void padWithZeroes(ByteBuffer buffer) {
        while (buffer.position() < buffer.limit()) {
            buffer.put((byte) 0);
        }
    }

    private ByteBuffer[] asFragmentArray(Pointer<Pointer<Byte>> fragmentPtrs, int numFragments, int fragmentSize) {
        ByteBuffer[] fragments = new ByteBuffer[numFragments];

        for (int i = 0; i < numFragments; i++) {
            fragments[i] = fragmentPtrs.get(i).getByteBuffer(fragmentSize);
        }

        return fragments;
    }

}
