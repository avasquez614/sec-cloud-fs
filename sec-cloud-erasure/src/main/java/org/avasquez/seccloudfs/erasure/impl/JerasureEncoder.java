package org.avasquez.seccloudfs.erasure.impl;

import org.avasquez.seccloudfs.erasure.EncodingResult;
import org.avasquez.seccloudfs.erasure.ErasureEncoder;

import java.nio.ByteBuffer;

/**
 * Implementation of {@link org.avasquez.seccloudfs.erasure.ErasureEncoder} that uses Jerasure.
 *
 * @author avasquez
 */
public class JerasureEncoder implements ErasureEncoder {

    @Override
    public EncodingResult encode(ByteBuffer rawData) {
        return null;
    }

    public static void main(String... args) {
        Liberation liberation = new Liberation();
        liberation.setK(6);
        liberation.setM(2);
        liberation.setW(7);
        liberation.setPacketSize(1024);
        liberation.init();
    }

}
