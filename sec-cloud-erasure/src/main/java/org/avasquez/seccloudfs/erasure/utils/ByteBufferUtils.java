package org.avasquez.seccloudfs.erasure.utils;

import org.bridj.Pointer;

import java.nio.ByteBuffer;

/**
 * Utility methods for byte buffers.
 *
 * @author avasquez
 */
public class ByteBufferUtils {

    private ByteBufferUtils() {
    }

    /**
     * Slices the buffer into {@code numSlices} number of slices with size {@code sliceSize}
     *
     * @param buffer    the buffer to slice
     * @param numSlices the number of slices
     * @param sliceSize the slice size
     *
     * @return the array of slices
     */
    public static ByteBuffer[] sliceBuffer(ByteBuffer buffer, int numSlices, int sliceSize) {
        ByteBuffer[] slices = new ByteBuffer[numSlices];

        buffer.clear();
        buffer.limit(sliceSize);

        for (int i = 0; i < numSlices; i++) {
            slices[i] = buffer.slice();

            if (i < numSlices - 1) {
                buffer.position(buffer.limit());
                buffer.limit(buffer.limit() + sliceSize);
            }
        }

        return slices;
    }

    /**
     * Returns the specified array of byte buffers as a BridJ array of pointers (byte**).
     *
     * @param buffers the byte buffers
     *
     * @return the array of pointers (byte**)
     */
    public static Pointer<Pointer<Byte>> asPointers(ByteBuffer[] buffers) {
        Pointer<Pointer<Byte>> ptrs = Pointer.allocatePointers(Byte.class, buffers.length);

        for (int i = 0; i < buffers.length; i++) {
            ptrs.set(i, Pointer.pointerToBytes(buffers[i]));
        }

        return ptrs;
    }

}
