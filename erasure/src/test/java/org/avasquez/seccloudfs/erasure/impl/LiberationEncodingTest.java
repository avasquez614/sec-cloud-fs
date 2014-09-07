package org.avasquez.seccloudfs.erasure.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.core.io.ClassPathResource;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

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

    @Test
    public void testEncodingAndDecoding() throws Exception {
        ClassPathResource resource = new ClassPathResource(FILE_PATH);
        int size = (int) resource.getFile().length();
        ReadableByteChannel inputChannel = Channels.newChannel(resource.getInputStream());
        FileChannel[] dataSlices = createTmpFileChannels(encoder.getK());
        FileChannel[] codingSlices = createTmpFileChannels(encoder.getM());

        int sliceSize = encoder.encode(inputChannel, size, dataSlices, codingSlices);

        assertEquals(dataSlices[0].size(), sliceSize);

        ByteArrayOutputStream output = new ByteArrayOutputStream(size);
        WritableByteChannel outputChannel = Channels.newChannel(output);

        resetChannels(dataSlices);
        resetChannels(codingSlices);

        decoder.decode(size, dataSlices, codingSlices, outputChannel);

        byte[] outputData = output.toByteArray();

        assertNotNull(outputData);
        assertEquals(size, outputData.length);
        assertArrayEquals(IOUtils.toByteArray(resource.getInputStream()), outputData);
    }

    @Test
    public void testEncodingAndDecodingWithMissingSlices() throws Exception {
        ClassPathResource resource = new ClassPathResource(FILE_PATH);
        byte[] originalData = IOUtils.toByteArray(resource.getInputStream());
        int size = originalData.length;
        ReadableByteChannel inputChannel = Channels.newChannel(new ByteArrayInputStream(originalData));
        FileChannel[] dataSlices = createTmpFileChannels(encoder.getK());
        FileChannel[] codingSlices = createTmpFileChannels(encoder.getM());

        int sliceSize = encoder.encode(inputChannel, size, dataSlices, codingSlices);

        assertEquals(dataSlices[0].size(), sliceSize);

        ByteArrayOutputStream output = new ByteArrayOutputStream(size);
        WritableByteChannel outputChannel = Channels.newChannel(output);

        dataSlices[2] = null;
        codingSlices[0] = null;

        resetChannels(dataSlices);
        resetChannels(codingSlices);

        decoder.decode(size, dataSlices, codingSlices, outputChannel);

        byte[] outputData = output.toByteArray();

        assertNotNull(outputData);
        assertEquals(size, outputData.length);
        assertArrayEquals(IOUtils.toByteArray(resource.getInputStream()), outputData);
    }

    private FileChannel[] createTmpFileChannels(int num) throws IOException {
        FileChannel[] channels = new FileChannel[num];

        for (int i = 0; i < num; i++) {
            Path path = tmpFolder.newFile().toPath();
            FileChannel channel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE);

            channels[i] = channel;
        }

        return channels;
    }

    private void resetChannels(FileChannel[] channels) throws IOException {
        for (FileChannel channel : channels) {
            if (channel != null) {
                channel.position(0);
            }
        }
    }

}
