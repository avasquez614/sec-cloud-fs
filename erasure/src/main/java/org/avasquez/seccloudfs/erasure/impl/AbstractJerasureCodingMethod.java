package org.avasquez.seccloudfs.erasure.impl;

import java.util.Arrays;
import javax.annotation.PostConstruct;

import org.bridj.Pointer;
import org.springframework.beans.factory.annotation.Required;

/**
 * Base for all {@link org.avasquez.seccloudfs.erasure.impl.JerasureCodingMethod}s.
 *
 * @author avasquez
 */
public abstract class AbstractJerasureCodingMethod implements JerasureCodingMethod {

    protected static final int[] prime55 = { 2,  3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67,
            71, 73, 79, 83, 89, 97,  101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151, 157, 163, 167, 173, 179,
            181, 191, 193, 197, 199, 211, 223, 227, 229, 233, 239, 241, 251, 257 };

    protected int k;
    protected int m;
    protected int w;
    protected int packetSize;

    protected Pointer<Integer> bitMatrix;
    protected Pointer<Pointer<Integer>> schedule;

    @Override
    public int getK() {
        return k;
    }

    @Required
    public void setK(int k) {
        this.k = k;
    }

    @Override
    public int getM() {
        return m;
    }

    @Required
    public void setM(int m) {
        this.m = m;
    }

    @Override
    public int getW() {
        return w;
    }

    @Required
    public void setW(int w) {
        this.w = w;
    }

    @Override
    public int getPacketSize() {
        return packetSize;
    }

    @Required
    public void setPacketSize(int packetSize) {
        this.packetSize = packetSize;
    }

    @PostConstruct
    public void init() {
        doInit();
    }

    @Override
    public void encode(Pointer<Pointer<Byte>> dataPtrs, Pointer<Pointer<Byte>> codingPtrs, int fragmentSize) {
        JerasureLibrary.scheduleEncode(k, m, w, schedule, dataPtrs, codingPtrs, fragmentSize, packetSize);
    }

    @Override
    public boolean decode(Pointer<Integer> erasures, Pointer<Pointer<Byte>> dataPtrs,
                          Pointer<Pointer<Byte>> codingPtrs, int sliceSize) {
        int result = JerasureLibrary.scheduleDecodeLazy(k, m, w, bitMatrix, erasures, dataPtrs, codingPtrs,
                sliceSize, packetSize, true);

        return result != -1;
    }

    protected boolean isPrime(int w) {
        return Arrays.binarySearch(prime55, w) >= 0;
    }

    protected abstract void doInit();
    
}
