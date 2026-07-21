package org.dreamjemu.gdrom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiscImageDetectorTest {

    @TempDir
    Path tempDir;

    @Test
    void detectsGdiByExtensionAndContent() throws IOException {
        Path file = tempDir.resolve("game.gdi");
        Files.writeString(file,
                "3\n" +
                        "1 0 4 2352 track01.bin 0\n" +
                        "2 750 0 2352 track02.raw 0\n" +
                        "3 900 4 2352 track03.bin 0\n",
                StandardCharsets.US_ASCII);

        assertEquals(DiscImageFormat.GDI, DiscImageDetector.detect(file));
    }

    @Test
    void detectsCueByExtensionAndContent() throws IOException {
        Path file = tempDir.resolve("game.cue");
        Files.writeString(file,
                "FILE \"game.bin\" BINARY\n" +
                        "  TRACK 01 MODE1/2352\n" +
                        "    INDEX 01 00:00:00\n",
                StandardCharsets.US_ASCII);

        assertEquals(DiscImageFormat.CUE_BIN, DiscImageDetector.detect(file));
    }

    @Test
    void detectsChdByMagicHeader() throws IOException {
        Path file = tempDir.resolve("game.chd");
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw")) {
            raf.write("MComprHD".getBytes(StandardCharsets.US_ASCII));
            raf.write(new byte[128]); // padding, irrelevant for detection
        }

        assertEquals(DiscImageFormat.CHD, DiscImageDetector.detect(file));
    }

    @Test
    void detectsCdiByTrailerMagic() throws IOException {
        Path file = tempDir.resolve("game.cdi");
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw")) {
            raf.write(new byte[256]); // arbitrary body content
            // little-endian 0x80000004 trailer marker (a real DiscJuggler v3 version marker)
            raf.write(new byte[]{0x04, 0x00, 0x00, (byte) 0x80});
        }

        assertEquals(DiscImageFormat.CDI, DiscImageDetector.detect(file));
    }

    @Test
    void detectsChdEvenWithWrongExtension() throws IOException {
        // Extension lies, but content still structurally matches CHD — detector
        // should fall back to content probing rather than trusting the extension blindly.
        Path file = tempDir.resolve("game.chd.bak");
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw")) {
            raf.write("MComprHD".getBytes(StandardCharsets.US_ASCII));
            raf.write(new byte[64]);
        }

        assertEquals(DiscImageFormat.CHD, DiscImageDetector.detect(file));
    }

    @Test
    void rejectsGdiExtensionWithGarbageContent() throws IOException {
        // Extension matches GDI, but the content doesn't have a valid track-count
        // first line — should not be misreported as a valid GDI.
        Path file = tempDir.resolve("not-really.gdi");
        Files.writeString(file, "this is not a track count\nrandom text\n", StandardCharsets.US_ASCII);

        assertEquals(DiscImageFormat.UNKNOWN, DiscImageDetector.detect(file));
    }

    @Test
    void returnsUnknownForCompletelyUnrelatedFile() throws IOException {
        Path file = tempDir.resolve("readme.txt");
        Files.writeString(file, "Just a plain text file, not a disc image.", StandardCharsets.US_ASCII);

        assertEquals(DiscImageFormat.UNKNOWN, DiscImageDetector.detect(file));
    }

    @Test
    void returnsUnknownForNonExistentFile() {
        Path file = tempDir.resolve("does-not-exist.gdi");
        assertEquals(DiscImageFormat.UNKNOWN, DiscImageDetector.detect(file));
    }
}
