package org.avasquez.seccloudfs.erasure.impl;

import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.avasquez.seccloudfs.erasure.ErasureDecoder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link org.avasquez.seccloudfs.erasure.impl.BufferedErasureEncoder}.
 *
 * @author avasquez
 */
public class BufferedErasureDecoderTest {

    @Mock
    private ErasureDecoder wrappedDecoder;
    private BufferedErasureDecoder decoder;

    @Before
    public void setUp() throws Exception {
        wrappedDecoder = mock(ErasureDecoder.class);

        decoder = new BufferedErasureDecoder();
        decoder.setActualDecoder(wrappedDecoder);
        decoder.setBufferSize(1024);
    }

    @Test
    public void testDecode() throws Exception {
        ReadableByteChannel[] dataSlices = new ReadableByteChannel[0];
        ReadableByteChannel[] codingSlices = new ReadableByteChannel[0];
        WritableByteChannel output = mock(WritableByteChannel.class);

        decoder.decode(3000, dataSlices, codingSlices, output);

        verify(wrappedDecoder, times(2)).decode(1024, dataSlices, codingSlices, output);
        verify(wrappedDecoder).decode(952, dataSlices, codingSlices, output);
    }

    @Test
    public void testDecodeWithSizeSmallerThanBufferSize() throws Exception {
        ReadableByteChannel[] dataSlices = new ReadableByteChannel[0];
        ReadableByteChannel[] codingSlices = new ReadableByteChannel[0];
        WritableByteChannel output = mock(WritableByteChannel.class);

        decoder.decode(800, dataSlices, codingSlices, output);

        verify(wrappedDecoder).decode(800, dataSlices, codingSlices, output);
    }

}
