package org.avasquez.seccloudfs.erasure.impl;

import org.bridj.BridJ;
import org.bridj.CRuntime;
import org.bridj.Pointer;
import org.bridj.ann.Library;
import org.bridj.ann.Name;
import org.bridj.ann.Ptr;
import org.bridj.ann.Runtime;
import org.bridj.util.DefaultParameterizedType;

/**
 * Interface to Jerasure's C library, using BridJ. Only used methods are included.
 *
 * @author avasquez
 */
@Library("Jerasure")
@Runtime(CRuntime.class)
public class JerasureLibrary {

    static {
        BridJ.register();
    }

    /**
     * Original signature : <code>int** jerasure_smart_bitmatrix_to_schedule(int, int, int, int*)</code><br>
     * <i>native declaration : /usr/local/include/jerasure.h:81</i>
     */
    public static Pointer<Pointer<Integer>> jerasure_smart_bitmatrix_to_schedule(int k, int m, int w,
                                                                                 Pointer<Integer> bitmatrix) {
        return Pointer.pointerToAddress(jerasure_smart_bitmatrix_to_schedule(k, m, w, Pointer.getPeer(bitmatrix)),
                DefaultParameterizedType.paramType(Pointer.class, Integer.class));
    }

    @Ptr
    protected native static long jerasure_smart_bitmatrix_to_schedule(int k, int m, int w, @Ptr long bitmatrix);

    /**
     * Original signature : <code>int* liberation_coding_bitmatrix(int, int)</code><br>
     * <i>native declaration : /usr/local/include/jerasure/liberation.h:46</i>
     */
    public static Pointer<Integer> liberation_coding_bitmatrix(int k, int w) {
        return Pointer.pointerToAddress(liberation_coding_bitmatrix$2(k, w), Integer.class);
    }

    @Ptr
    @Name("liberation_coding_bitmatrix")
    protected native static long liberation_coding_bitmatrix$2(int k, int w);

}
