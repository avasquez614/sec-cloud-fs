package org.avasquez.seccloudfs.erasure.impl;

import org.apache.commons.io.IOUtils;
import org.avasquez.seccloudfs.erasure.Fragments;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import static org.junit.Assert.*;

/**
 * Unit tests for Jerasure liberation encoding/decoding.
 *
 * @author avasquez
 */
public class LiberationEncodingTest {

    private static final int K =            6;
    private static final int M =            2;
    private static final int W =            7;
    private static final int PACKET_SIZE =  8;

    private static final String FILE_PATH = "gpl-3.0.txt";

    private JerasureEncoder encoder;
    private JerasureDecoder decoder;
    private Liberation liberation;

    @Before
    public void setUp() throws Exception {
        liberation = new Liberation();
        liberation.setK(K);
        liberation.setM(M);
        liberation.setW(W);
        liberation.setPacketSize(PACKET_SIZE);
        liberation.init();

        encoder = new JerasureEncoder();
        decoder = new JerasureDecoder();

        encoder.setCodingMethod(liberation);
        decoder.setCodingMethod(liberation);
    }

    //@Test
    public void testEncodingAndDecoding() throws Exception {
        ClassPathResource resource = new ClassPathResource(FILE_PATH);
        int size = (int) resource.getFile().length();
        ReadableByteChannel inputChannel = Channels.newChannel(resource.getInputStream());

        Fragments fragments = encoder.encode(inputChannel, size);

        assertNotNull(fragments);

        ByteBuffer[] dataFragments = fragments.getDataFragments();
        ByteBuffer[] codingFragments = fragments.getCodingFragments();

        assertNotNull(dataFragments);
        assertEquals(K, dataFragments.length);

        assertNotNull(codingFragments);
        assertEquals(M, codingFragments.length);

        ByteArrayOutputStream output = new ByteArrayOutputStream(size);
        WritableByteChannel outputChannel = Channels.newChannel(output);

        decoder.decode(fragments, size, outputChannel);

        byte[] outputData = output.toByteArray();

        assertNotNull(outputData);
        assertEquals(size, outputData.length);
        assertArrayEquals(IOUtils.toByteArray(resource.getInputStream()), outputData);
    }

    @Test
    public void testEncodingAndDecodingWithMissingFragments() throws Exception {
        ClassPathResource resource = new ClassPathResource(FILE_PATH);
        byte[] originalData = IOUtils.toByteArray(resource.getInputStream());
        int size = originalData.length;
        ReadableByteChannel inputChannel = Channels.newChannel(new ByteArrayInputStream(originalData));

        Fragments fragments = encoder.encode(inputChannel, size);

        assertNotNull(fragments);

        ByteBuffer[] dataFragments = fragments.getDataFragments();
        ByteBuffer[] codingFragments = fragments.getCodingFragments();

        assertNotNull(dataFragments);
        assertEquals(K, dataFragments.length);

        assertNotNull(codingFragments);
        assertEquals(M, codingFragments.length);

        dataFragments[2] = null;
        codingFragments[0] = null;

        ByteArrayOutputStream output = new ByteArrayOutputStream(size);
        WritableByteChannel outputChannel = Channels.newChannel(output);

        decoder.decode(fragments, size, outputChannel);

        byte[] outputData = output.toByteArray();

        assertNotNull(outputData);
        assertEquals(size, outputData.length);
        assertArrayEquals(originalData, outputData);

        resetBuffers(dataFragments);

        output = new ByteArrayOutputStream(size);
        outputChannel = Channels.newChannel(output);

        decoder.decode(fragments, size, outputChannel);

        outputData = output.toByteArray();

        assertNotNull(outputData);
        assertEquals(size, outputData.length);
        assertArrayEquals(originalData, outputData);
    }

    private void resetBuffers(ByteBuffer[] buffers) {
        for (ByteBuffer buffer : buffers) {
            buffer.clear();
        }
    }

}
