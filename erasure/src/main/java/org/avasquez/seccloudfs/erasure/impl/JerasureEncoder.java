package org.avasquez.seccloudfs.erasure.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.avasquez.seccloudfs.erasure.EncodingException;
import org.avasquez.seccloudfs.erasure.ErasureEncoder;
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
    public int getK() {
        return codingMethod.getK();
    }

    @Override
    public int getM() {
        return codingMethod.getM();
    }

    @Override
    public int encode(ReadableByteChannel input, int size, WritableByteChannel[] dataSlices,
                      WritableByteChannel[] codingSlices) throws EncodingException {
        int k = codingMethod.getK();
        int m = codingMethod.getM();

        if (dataSlices.length != k) {
            throw new EncodingException("Illegal length of data slices array (expected: " + k + ", actual: " +
                dataSlices.length + ")");
        }
        if (codingSlices.length != m) {
            throw new EncodingException("Illegal length of coding slices array (expected: " + m + ", actual: " +
                codingSlices.length + ")");
        }

        int dataBufferSize = codingMethod.getPaddedSize(size);
        ByteBuffer dataBuffer = ByteBuffer.allocateDirect(dataBufferSize);

        dataBuffer.limit(size);

        // Read data into buffer
        try {
            input.read(dataBuffer);
        } catch (IOException e) {
            throw new EncodingException("Unable to read data from input", e);
        }

        dataBuffer.limit(dataBufferSize);

        // Pad the rest with zeroes
        padWithZeroes(dataBuffer);

        // Create pointers to data
        int sliceSize = dataBufferSize / k;
        ByteBuffer[] dataBuffers = ByteBufferUtils.sliceBuffer(dataBuffer, k,  sliceSize);
        Pointer<Pointer<Byte>> dataPtrs = ByteBufferUtils.asPointers(dataBuffers);

        // Allocate memory for coding and create pointers to coding
        ByteBuffer codingBuffer = ByteBuffer.allocateDirect(m * sliceSize);
        ByteBuffer[] codingBuffers = ByteBufferUtils.sliceBuffer(codingBuffer, m, sliceSize);
        Pointer<Pointer<Byte>> codingPtrs = ByteBufferUtils.asPointers(codingBuffers);

        // Do encoding
        codingMethod.encode(dataPtrs, codingPtrs, sliceSize);

        try {
            writeBuffers(dataBuffers, dataSlices);
        } catch (IOException e) {
            throw new EncodingException("Unable to write data slice buffers to channels", e);
        }
        try {
            writeBuffers(codingBuffers, codingSlices);
        } catch (IOException e) {
            throw new EncodingException("Unable to write coding slice buffers to channels", e);
        }

        return sliceSize;
    }

    private void padWithZeroes(ByteBuffer buffer) {
        while (buffer.position() < buffer.limit()) {
            buffer.put((byte) 0);
        }
    }

    private void writeBuffers(ByteBuffer[] buffers, WritableByteChannel[] channels) throws IOException {
        for (int i = 0; i < buffers.length; i++) {
            buffers[i].clear();
            channels[i].write(buffers[i]);
        }
    }

}
