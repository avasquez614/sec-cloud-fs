package org.avasquez.seccloudfs.erasure.impl;

import org.avasquez.seccloudfs.erasure.DecodingException;
import org.avasquez.seccloudfs.erasure.ErasureDecoder;
import org.avasquez.seccloudfs.erasure.Fragments;
import org.bridj.Pointer;
import org.springframework.beans.factory.annotation.Required;

import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;

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
        int w = codingMethod.getW();
        int packetSize = codingMethod.getPacketSize();
        ByteBuffer[] dataFragments = fragments.getDataFragments();
        ByteBuffer[] codingFragments = fragments.getCodingFragments();
        List<Integer> erasures = new ArrayList<>(k + m);
        int fragmentSize = 0;

        // Look for erasures in data fragments
        for (int i = 0; i < dataFragments.length; i++) {
            if (dataFragments[i] == null) {
                erasures.add(i);
            } else if (fragmentSize == 0) {
                fragmentSize = dataFragments[i].capacity();
            }
        }
        // Look for erasures in coding fragments
        for (int i = 0; i < codingFragments.length; i++) {
            if (codingFragments[i] == null) {
                erasures.add(k + i);
            } else if (fragmentSize == 0) {
                fragmentSize = codingFragments[i].capacity();
            }
        }

        if (erasures.size() > m) {
            throw new DecodingException("Unable to continue decoding: more than m (" + m + ") fragments are missing");
        }

        erasures.add(-1);

        // Get pointers for data and coding fragments;
        Pointer<Pointer<Byte>> dataPtrs = asPointers(dataFragments, k, fragmentSize);
        Pointer<Pointer<Byte>> codingPtrs = asPointers(codingFragments, m, fragmentSize);
    }

    private Pointer<Pointer<Byte>> asPointers(ByteBuffer[] fragments, int numFragments, int fragmentSize) {
        Pointer<Pointer<Byte>> ptrs = Pointer.allocatePointers(Byte.class, numFragments);
        for (int i = 0; i < fragments.length; i++) {
            if (fragments[i] == null) {
                // Allocate memory for missing fragments, which will be reconstructed by the decoder
                ptrs.set(i, Pointer.allocateBytes(fragmentSize));
            } else {
                ptrs.set(i, Pointer.pointerToBytes(fragments[i]));
            }
        }

        return ptrs;
    }

}
