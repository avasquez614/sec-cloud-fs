package org.avasquez.seccloudfs.erasure.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.avasquez.seccloudfs.erasure.DecodingException;
import org.avasquez.seccloudfs.erasure.ErasureDecoder;
import org.avasquez.seccloudfs.erasure.utils.ByteBufferUtils;
import org.bridj.Pointer;
import org.springframework.beans.factory.annotation.Required;

/**
 * Implementation of {@link org.avasquez.seccloudfs.erasure.ErasureDecoder} that uses Jerasure.
 *
 * @author avasquez
 */
public class JerasureDecoder implements ErasureDecoder {

    private JerasureCodingMethod codingMethod;

    @Required
    public void setCodingMethod(JerasureCodingMethod codingMethod) {
        this.codingMethod = codingMethod;
    }

    @Override
    public int getK() {
        return codingMethod.getK();
    }

    @Override
    public int getM() {
        return codingMethod.getM();
    }

    @Override
    public void decode(int originalSize, ReadableByteChannel[] dataSlices, ReadableByteChannel[] codingSlices,
                       WritableByteChannel output) throws DecodingException {
        int k = codingMethod.getK();
        int m = codingMethod.getM();

        if (dataSlices.length != k) {
            throw new DecodingException("Illegal length of data slices array (expected: " + k + ", actual: " +
                dataSlices.length + ")");
        }
        if (codingSlices.length != m) {
            throw new DecodingException("Illegal length of coding slices array (expected: " + m + ", actual: " +
                codingSlices.length + ")");
        }

        int sliceSize = getSliceSize(originalSize);
        ByteBuffer[] dataBuffers = createBuffers(dataSlices, sliceSize);
        ByteBuffer[] codingBuffers = createBuffers(codingSlices, sliceSize);
        int[] erasures = new int[k + m];
        int numErased = 0;

        // Look for erasures in data slices
        for (int i = 0; i < dataBuffers.length; i++) {
            if (dataBuffers[i] == null) {
                erasures[numErased] = i;
                numErased++;
            }
        }

        // If no data slices have been erased, just write them to the output channel
        if (numErased > 0) {
            // Look for erasures in coding slices
            for (int i = 0; i < codingBuffers.length; i++) {
                if (codingBuffers[i] == null) {
                    erasures[numErased] = k + i;
                    numErased++;
                } else if (sliceSize == 0) {
                    sliceSize = codingBuffers[i].capacity();
                }
            }

            erasures[numErased] = -1;

            if (numErased > m) {
                throw new DecodingException("More than m (" + m + ") slices are missing");
            }

            // Allocate memory for missing slices, which will be reconstructed by the decoder
            for (int i = 0; i < numErased; i++) {
                if (erasures[i] < k) {
                    dataBuffers[erasures[i]] = ByteBuffer.allocateDirect(sliceSize);
                } else {
                    codingBuffers[erasures[i] - k] = ByteBuffer.allocateDirect(sliceSize);
                }
            }

            // Get pointers for data and coding slices;
            Pointer<Pointer<Byte>> dataPtrs = ByteBufferUtils.asPointers(dataBuffers);
            Pointer<Pointer<Byte>> codingPtrs = ByteBufferUtils.asPointers(codingBuffers);

            // Do decoding
            boolean success = codingMethod.decode(Pointer.pointerToInts(erasures), dataPtrs, codingPtrs, sliceSize);
            if (!success) {
                throw new DecodingException("Decoding failed for unknown reasons");
            }
        }

        // Write completed data slices to output channel, until original size has been written
        int totalWritten = 0;
        for (int i = 0; i < dataBuffers.length; i++) {
            if (totalWritten + sliceSize > originalSize) {
                // This is the slice with padded zeroes, so just write the actual bytes
                dataBuffers[i].limit(originalSize - totalWritten);
            }

            try {
                totalWritten += output.write(dataBuffers[i]);
            } catch (IOException e) {
                throw new DecodingException("Unable to write data to output", e);
            }
        }
    }

    private int getSliceSize(int dataSize) {
        return codingMethod.getPaddedSize(dataSize) / getK();
    }

    private ByteBuffer[] createBuffers(ReadableByteChannel[] channels, int sliceSize) throws DecodingException {
        ByteBuffer[] buffers = new ByteBuffer[channels.length];

        for (int i = 0; i < channels.length; i++) {
            ReadableByteChannel channel = channels[i];
            if (channel != null) {
                try {
                    ByteBuffer buffer = ByteBuffer.allocateDirect(sliceSize);
                    channel.read(buffer);

                    buffer.clear();

                    buffers[i] = buffer;
                } catch (IOException e) {
                    throw new DecodingException("Error copying channel to byte buffer", e);
                }
            } else {
                buffers[i] = null;
            }
        }

        return buffers;
    }

}
