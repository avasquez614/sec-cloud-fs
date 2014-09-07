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
     * Returns the size plus necessary pad size.
     *
     * @param size the original size
     *
     * @return size + pad size
     */
    int getPaddedSize(int size);

    /**
     * Encodes the data slices (k) into the coding slices (m).
     *
     * @param dataPtrs      pointers to the data slices
     * @param codingPtrs    pointers to the coding slices
     * @param sliceSize     the size of a data or coding slice
     */
    void encode(Pointer<Pointer<Byte>> dataPtrs, Pointer<Pointer<Byte>> codingPtrs, int sliceSize);

    /**
     * Decodes the remaining data (k) and coding slices into the original data slices.
     *
     * @param erasures      array with the missing (erased) slice IDs
     * @param dataPtrs      pointers to the data slices
     * @param codingPtrs    pointers to the coding slices
     * @param sliceSize     the size of a data or coding slice
     *
     * @return false if the decode was unsuccessful
     */
    boolean decode(Pointer<Integer> erasures, Pointer<Pointer<Byte>> dataPtrs, Pointer<Pointer<Byte>> codingPtrs,
                   int sliceSize);

}
