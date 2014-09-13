package org.avasquez.seccloudfs.erasure.impl;

import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.avasquez.seccloudfs.erasure.ErasureEncoder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.avasquez.seccloudfs.erasure.impl.BufferedErasureEncoder}.
 *
 * @author avasquez
 */
public class BufferedErasureEncoderTest {

    @Mock
    private ErasureEncoder wrappedEncoder;
    private BufferedErasureEncoder encoder;

    @Before
    public void setUp() throws Exception {
        wrappedEncoder = mock(ErasureEncoder.class);

        when(wrappedEncoder.encode(any(ReadableByteChannel.class), anyInt(), any(WritableByteChannel[].class),
            any(WritableByteChannel[].class))).then(new Answer<Integer>() {

            @Override
            public Integer answer(final InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                int size = (Integer) args[1];

                return (size / 4) + 64;
            }

        });

        encoder = new BufferedErasureEncoder();
        encoder.setActualEncoder(wrappedEncoder);
        encoder.setBufferSize(1024);
    }

    @Test
    public void testEncode() throws Exception {
        ReadableByteChannel input = mock(ReadableByteChannel.class);
        WritableByteChannel[] dataSlices = new WritableByteChannel[0];
        WritableByteChannel[] codingSlices = new WritableByteChannel[0];

        int sliceSize = encoder.encode(input, 3000, dataSlices, codingSlices);

        assertEquals(942, sliceSize);

        verify(wrappedEncoder, times(2)).encode(input, 1024, dataSlices, codingSlices);
        verify(wrappedEncoder).encode(input, 952, dataSlices, codingSlices);
    }

    @Test
    public void testEncodeWithSizeSmallerThanBufferSize() throws Exception {
        ReadableByteChannel input = mock(ReadableByteChannel.class);
        WritableByteChannel[] dataSlices = new WritableByteChannel[0];
        WritableByteChannel[] codingSlices = new WritableByteChannel[0];

        int sliceSize = encoder.encode(input, 800, dataSlices, codingSlices);

        assertEquals(264, sliceSize);

        verify(wrappedEncoder).encode(input, 800, dataSlices, codingSlices);
    }

}
