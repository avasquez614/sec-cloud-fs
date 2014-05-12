package org.avasquez.seccloudfs.erasure.impl;

import org.bridj.Pointer;

/**
 * Represents a Jerasure coding method (Cauchy, Liberation, etc.)
 *
 * @author avasquez
 */
public interface JerasureCodingMethod {

    /**
     * Returns the number of data fragments (k).
     */
    int getK();

    /**
     * Returns the number of coding fragments (m).
     */
    int getM();

    /**
     * Returns the word size
     */
    int getW();

    /**
     * Returns the packet size
     */
    int getPacketSize();

    /**
     * Encodes the data fragments (k) into the coding fragments (m).
     *
     * @param dataPtrs      pointers to the data fragments
     * @param codingPtrs    pointers to the coding fragments
     * @param fragmentSize  the size of a data or coding fragment
     */
    void encode(Pointer<Pointer<Byte>> dataPtrs, Pointer<Pointer<Byte>> codingPtrs, int fragmentSize);

    /**
     * Decodes the remaining data (k) and coding fragments into the original data fragments.
     *
     * @param erasures      array with the missing (erased) fragment IDs
     * @param dataPtrs      pointers to the data fragments
     * @param codingPtrs    pointers to the coding fragments
     * @param fragmentSize  the size of a data or coding fragment
     *
     * @return false if the decode was unsuccessful
     */
    boolean decode(Pointer<Integer> erasures, Pointer<Pointer<Byte>> dataPtrs, Pointer<Pointer<Byte>> codingPtrs,
                   int fragmentSize);

}
