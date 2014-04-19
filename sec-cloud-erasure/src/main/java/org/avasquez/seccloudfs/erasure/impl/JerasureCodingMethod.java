package org.avasquez.seccloudfs.erasure.impl;

import org.bridj.Pointer;

/**
 * Represents a Jerasure coding method (Cauchy, Liberation, etc.)
 *
 * @author avasquez
 */
public interface JerasureCodingMethod {

    int getFragmentSize();

    void encode(Pointer<Pointer<Byte>> dataPtrs, Pointer<Pointer<Byte>> codingPtrs);

}
