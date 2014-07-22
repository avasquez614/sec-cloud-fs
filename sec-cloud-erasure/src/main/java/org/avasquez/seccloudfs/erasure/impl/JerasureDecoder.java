package org.avasquez.seccloudfs.erasure.impl;

import org.avasquez.seccloudfs.erasure.DecodingException;
import org.avasquez.seccloudfs.erasure.ErasureDecoder;
import org.avasquez.seccloudfs.erasure.Slices;
import org.avasquez.seccloudfs.erasure.utils.ByteBufferUtils;
import org.bridj.Pointer;
import org.springframework.beans.factory.annotation.Required;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

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
    public void decode(Slices slices, int originalSize, WritableByteChannel outputChannel)
            throws DecodingException {
        int k = codingMethod.getK();
        int m = codingMethod.getM();
        ByteBuffer[] dataSlices = slices.getDataSlices();
        ByteBuffer[] codingSlices = slices.getCodingSlices();
        int[] erasures = new int[k + m];
        int numErased = 0;
        int sliceSize = 0;

        // Look for erasures in data slices
        for (int i = 0; i < dataSlices.length; i++) {
            if (dataSlices[i] == null) {
                erasures[numErased] = i;
                numErased++;
            } else if (sliceSize == 0) {
                sliceSize = dataSlices[i].capacity();
            }
        }

        // If no data slices have been erased, just write them to the output channel
        if (numErased > 0) {
            // Look for erasures in coding slices
            for (int i = 0; i < codingSlices.length; i++) {
                if (codingSlices[i] == null) {
                    erasures[numErased] = k + i;
                    numErased++;
                } else if (sliceSize == 0) {
                    sliceSize = codingSlices[i].capacity();
                }
            }

            erasures[numErased] = -1;

            if (numErased > m) {
                throw new DecodingException("More than m (" + m + ") slices are missing");
            }

            // Allocate memory for missing slices, which will be reconstructed by the decoder
            for (int i = 0; i < numErased; i++) {
                if (erasures[i] < k) {
                    dataSlices[erasures[i]] = ByteBuffer.allocateDirect(sliceSize);
                } else {
                    codingSlices[erasures[i] - k] = ByteBuffer.allocateDirect(sliceSize);
                }
            }

            // Get pointers for data and coding slices;
            Pointer<Pointer<Byte>> dataPtrs = ByteBufferUtils.asPointers(dataSlices);
            Pointer<Pointer<Byte>> codingPtrs = ByteBufferUtils.asPointers(codingSlices);

            // Do decoding
            boolean success = codingMethod.decode(Pointer.pointerToInts(erasures), dataPtrs, codingPtrs, sliceSize);
            if (!success) {
                throw new DecodingException("Decoding failed for unknown reasons");
            }
        }

        // Write completed data slices to output channel, until original size has been written
        int totalWritten = 0;
        for (int i = 0; i < dataSlices.length; i++) {
            if (totalWritten + sliceSize > originalSize) {
                // This is the slice with padded zeroes, so just write the actual bytes
                dataSlices[i].limit(originalSize - totalWritten);
            }

            try {
                totalWritten += outputChannel.write(dataSlices[i]);
            } catch (IOException e) {
                throw new DecodingException("Unable to write data to output channel", e);
            }
        }
    }

}
