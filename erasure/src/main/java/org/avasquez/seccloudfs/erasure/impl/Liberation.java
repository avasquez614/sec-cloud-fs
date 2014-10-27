package org.avasquez.seccloudfs.erasure.impl;

import org.bridj.CLong;

/**
 * Implementation of Jerasure Liberation coding method.
 *
 * @author avasquez
 */
public class Liberation extends AbstractJerasureCodingMethod {

    public void doInit() {
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

        m = 2;
        bitMatrix = JerasureLibrary.liberationCodingBitmatrix(k, w);
        schedule = JerasureLibrary.smartBitmatrixToSchedule(k, m, w, bitMatrix);
    }

    @Override
    public void setM(int m) {
        if (m == 2) {
            this.m = m;
        } else {
            throw new IllegalArgumentException("m should always be 2 in Liberation coding");
        }
    }

}
