package org.avasquez.seccloudfs.erasure.impl;

import org.bridj.CLong;
import org.bridj.Pointer;

import java.util.Arrays;

/**
 * Implementation of Jerasure Liberation coding method.
 *
 * @author avasquez
 */
public class Liberation implements JerasureCodingMethod {

    private static final int[] prime55 = { 2,3,5,7,11,13,17,19,23,29,31,37,41,43,47,53,59,61,67,71, 73,79,83,89,97,
            101,103,107,109,113,127,131,137,139,149,151,157,163,167,173,179,181,191,193,197,199,211,223,227,229,233,
            239,241,251,257 };

    private int k;
    private int m;
    private int w;
    private int packetSize;

    private Pointer<Integer> bitMatrix;
    private Pointer<Pointer<Integer>> schedule;

    public void setK(int k) {
        this.k = k;
    }

    public void setM(int m) {
        this.m = m;
    }

    public void setW(int w) {
        this.w = w;
    }

    public void setPacketSize(int packetSize) {
        this.packetSize = packetSize;
    }

    public void init() {
        if (k == 0) {
            throw new IllegalStateException("k must be specified");
        }
        if (m == 0) {
            throw new IllegalStateException("m must be specified");
        }
        if (w == 0) {
            throw new IllegalStateException("w must be specified");
        }
        if (k > w) {
            throw new IllegalStateException("k must be less than or equal to w");
        }
        if (w <= 2 || (w % 2 == 0) || !isPrime(w)) {
            throw new IllegalStateException("w must be greater than two and w must be prime");
        }
        if (packetSize == 0) {
            throw new IllegalStateException("packetSize must be specified");
        }
        if ((packetSize % CLong.SIZE) != 0) {
            throw new IllegalStateException("packetSize must be a multiple of " + CLong.SIZE);
        }

        bitMatrix = JerasureLibrary.liberation_coding_bitmatrix(k, w);
        schedule = JerasureLibrary.jerasure_smart_bitmatrix_to_schedule(k, m, w, bitMatrix);
    }

    @Override
    public int getFragmentSize() {
        return 0;
    }

    @Override
    public void encode(Pointer<Pointer<Byte>> dataPtrs, Pointer<Pointer<Byte>> codingPtrs) {

    }

    private boolean isPrime(int w) {
        return Arrays.binarySearch(prime55, w) >= 0;
    }

}
