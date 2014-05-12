package org.avasquez.seccloudfs.erasure.impl;

import org.avasquez.seccloudfs.erasure.DecodingException;
import org.avasquez.seccloudfs.erasure.ErasureDecoder;
import org.avasquez.seccloudfs.erasure.Fragments;
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
    public void decode(Fragments fragments, int originalSize, WritableByteChannel outputChannel)
            throws DecodingException {
        int k = codingMethod.getK();
        int m = codingMethod.getM();
        ByteBuffer[] dataFragments = fragments.getDataFragments();
        ByteBuffer[] codingFragments = fragments.getCodingFragments();
        int[] erasures = new int[k + m];
        int numErased = 0;
        int fragmentSize = 0;

        // Look for erasures in data fragments
        for (int i = 0; i < dataFragments.length; i++) {
            if (dataFragments[i] == null) {
                erasures[numErased] = i;
                numErased++;
            } else if (fragmentSize == 0) {
                fragmentSize = dataFragments[i].capacity();
            }
        }
        // Look for erasures in coding fragments
        for (int i = 0; i < codingFragments.length; i++) {
            if (codingFragments[i] == null) {
                erasures[numErased] = k + i;
                numErased++;
            } else if (fragmentSize == 0) {
                fragmentSize = codingFragments[i].capacity();
            }
        }

        erasures[numErased] = -1;

        if (numErased > m) {
            throw new DecodingException("Unable to continue decoding: more than m (" + m + ") fragments are missing");
        }

        // Allocate memory for missing fragments, which will be reconstructed by the decoder
        for (int i = 0; i < numErased; i++) {
            if (erasures[i] < k) {
                dataFragments[erasures[i]] = ByteBuffer.allocateDirect(fragmentSize);
            } else {
                codingFragments[erasures[i] - k] = ByteBuffer.allocateDirect(fragmentSize);
            }
        }

        // Get pointers for data and coding fragments;
        Pointer<Pointer<Byte>> dataPtrs = asPointers(dataFragments, k);
        Pointer<Pointer<Byte>> codingPtrs = asPointers(codingFragments, m);

        // Do decoding
        boolean success = codingMethod.decode(Pointer.pointerToInts(erasures), dataPtrs, codingPtrs, fragmentSize);
        if (!success) {
            throw new DecodingException("Decoding failed for unknown reasons");
        }

        // Write completed data fragments to output channel, until original size has been written
        int totalWritten = 0;
        for (int i = 0; i < dataFragments.length; i++) {
            if (totalWritten + fragmentSize > originalSize) {
                // This is the fragment with padded zeroes, so just write the actual bytes
                dataFragments[i].limit(originalSize - totalWritten);
            }

            try {
                totalWritten += outputChannel.write(dataFragments[i]);
            } catch (IOException e) {
                throw new DecodingException("Unable to write data to output channel", e);
            }
        }
    }

    private Pointer<Pointer<Byte>> asPointers(ByteBuffer[] fragments, int numFragments) {
        Pointer<Pointer<Byte>> ptrs = Pointer.allocatePointers(Byte.class, numFragments);
        for (int i = 0; i < fragments.length; i++) {
            ptrs.set(i, Pointer.pointerToBytes(fragments[i]));
        }

        return ptrs;
    }

}
