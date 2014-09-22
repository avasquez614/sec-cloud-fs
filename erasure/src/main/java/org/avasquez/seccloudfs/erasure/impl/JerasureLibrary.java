package org.avasquez.seccloudfs.erasure.impl;

import java.lang.reflect.Type;

import org.bridj.BridJ;
import org.bridj.CRuntime;
import org.bridj.Pointer;
import org.bridj.ann.Library;
import org.bridj.ann.Ptr;
import org.bridj.ann.Runtime;
import org.bridj.util.DefaultParameterizedType;

/**
 * Interface to Jerasure's C library, using BridJ. Only used methods are included.
 *
 * @author avasquez
 */
@Library(value = "Jerasure", dependencies = { "gf_complete" })
@Runtime(CRuntime.class)
public class JerasureLibrary {

    static {
        BridJ.register();
    }

    public static Pointer<Integer> cauchyGoodGeneralCodingMatrix(int k, int m, int w) {
        long address = cauchy_good_general_coding_matrix(k, m, w);

        return Pointer.pointerToAddress(address, Integer.class, MatrixReleaser.INSTANCE);
    }

    public static Pointer<Integer> liberationCodingBitmatrix(int k, int w) {
        long address = liberation_coding_bitmatrix(k, w);

        return Pointer.pointerToAddress(address, Integer.class, MatrixReleaser.INSTANCE);
    }

    public static Pointer<Integer> matrixToBitmatrix(int k, int m, int w, Pointer<Integer> matrix) {
        long address = jerasure_matrix_to_bitmatrix(k, m, w, Pointer.getPeer(matrix));

        return Pointer.pointerToAddress(address, Integer.class, MatrixReleaser.INSTANCE);
    }

    public static Pointer<Pointer<Integer>> smartBitmatrixToSchedule(int k, int m, int w, Pointer<Integer> bitmatrix) {
        long address = jerasure_smart_bitmatrix_to_schedule(k, m, w, Pointer.getPeer(bitmatrix));
        Type type = DefaultParameterizedType.paramType(Pointer.class, Integer.class);

        return Pointer.pointerToAddress(address, type, ScheduleReleaser.INSTANCE);
    }

    public static void scheduleEncode(int k, int m, int w, Pointer<Pointer<Integer>> schedule,
                                      Pointer<Pointer<Byte>> dataPtrs, Pointer<Pointer<Byte>> codingPtrs,
                                      int size, int packetSize) {
        long scheduleAddress = Pointer.getPeer(schedule);
        long dataPtrsAddress = Pointer.getPeer(dataPtrs);
        long codingPtrsAddress = Pointer.getPeer(codingPtrs);

        jerasure_schedule_encode(k, m, w, scheduleAddress, dataPtrsAddress, codingPtrsAddress, size, packetSize);
    }

    public static int scheduleDecodeLazy(int k, int m, int w, Pointer<Integer> bitmatrix, Pointer<Integer> erasures,
                                         Pointer<Pointer<Byte>> dataPtrs, Pointer<Pointer<Byte>> codingPtrs, int size,
                                         int packetSize, boolean smart) {
        long bitmatrixAddress = Pointer.getPeer(bitmatrix);
        long erasuresAddress = Pointer.getPeer(erasures);
        long dataPtrsAddress = Pointer.getPeer(dataPtrs);
        long codingPtrsAddress = Pointer.getPeer(codingPtrs);

        return jerasure_schedule_decode_lazy(k, m, w, bitmatrixAddress, erasuresAddress, dataPtrsAddress,
                codingPtrsAddress, size, packetSize, 1);
    }

    @Ptr
    protected native static long cauchy_good_general_coding_matrix(int k, int m, int w);
    @Ptr
    protected native static long liberation_coding_bitmatrix(int k, int w);
    @Ptr
    protected native static long jerasure_matrix_to_bitmatrix(int k, int m, int w, @Ptr long matrix);
    @Ptr
    protected native static long jerasure_smart_bitmatrix_to_schedule(int k, int m, int w, @Ptr long bitmatrix);

    protected native static void jerasure_free_matrix(@Ptr long matrix);

    protected native static void jerasure_free_schedule(@Ptr long schedule);

    protected native static void jerasure_schedule_encode(int k, int m, int w, @Ptr long schedule, @Ptr long data_ptrs,
                                                          @Ptr long coding_ptrs, int size, int packetsize);

    protected native static int jerasure_schedule_decode_lazy(int k, int m, int w, @Ptr long bitmatrix,
                                                              @Ptr long erasures, @Ptr long data_ptrs,
                                                              @Ptr long coding_ptrs, int size, int packetsize,
                                                              int smart);

    private static class MatrixReleaser implements Pointer.Releaser {

        public static final MatrixReleaser INSTANCE = new MatrixReleaser();

        private MatrixReleaser() {
        }

        @Override
        public void release(Pointer<?> ptr) {
            jerasure_free_matrix(Pointer.getPeer(ptr));
        }

    }

    private static class ScheduleReleaser implements Pointer.Releaser {

        public static final ScheduleReleaser INSTANCE = new ScheduleReleaser();

        private ScheduleReleaser() {
        }

        @Override
        public void release(Pointer<?> ptr) {
            jerasure_free_schedule(Pointer.getPeer(ptr));
        }

    }

}
