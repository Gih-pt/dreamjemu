package org.dreamjemu.gdrom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CueBinImageTest {

    @TempDir
    Path tempDir;

    private static void fillSector(byte[] array, int offset, int length, byte value) {
        for (int i = 0; i < length; i++) {
            array[offset + i] = value;
        }
    }

    /**
     * Builds a small, real two-track CUE/BIN image sharing a single .bin
     * file: a 2-sector MODE1/2352 data track followed by a 1-sector AUDIO
     * track, each with distinct, recognizable sector content, so reads can
     * be verified byte-for-byte. Mirrors the layout used by
     * {@code GdiImageTest#buildSampleGdi}.
     */
    private Path buildSingleFileSample() throws IOException {
        int sectorSize = 2352;

        byte[] bin = new byte[sectorSize * 3];
        fillSector(bin, 0, sectorSize, (byte) 0xAA);              // track 1, LBA 0
        fillSector(bin, sectorSize, sectorSize, (byte) 0xBB);     // track 1, LBA 1
        fillSector(bin, sectorSize * 2, sectorSize, (byte) 0xCC); // track 2, LBA 2

        Files.write(tempDir.resolve("game.bin"), bin);

        Path cue = tempDir.resolve("game.cue");
        Files.writeString(cue,
                "FILE \"game.bin\" BINARY\n" +
                        "  TRACK 01 MODE1/2352\n" +
                        "    INDEX 01 00:00:00\n" +
                        "  TRACK 02 AUDIO\n" +
                        "    INDEX 01 00:00:02\n",
                StandardCharsets.US_ASCII);
        return cue;
    }

    /**
     * Builds a two-track CUE/BIN image using one .bin file per track (the
     * other common real-world layout), each track starting at its own
     * file's beginning.
     */
    private Path buildSeparateFilesSample() throws IOException {
        int sectorSize = 2352;

        byte[] track1 = new byte[sectorSize * 2];
        fillSector(track1, 0, sectorSize, (byte) 0x11);
        fillSector(track1, sectorSize, sectorSize, (byte) 0x22);

        byte[] track2 = new byte[sectorSize];
        fillSector(track2, 0, sectorSize, (byte) 0x33);

        Files.write(tempDir.resolve("track01.bin"), track1);
        Files.write(tempDir.resolve("track02.bin"), track2);

        Path cue = tempDir.resolve("separate.cue");
        Files.writeString(cue,
                "FILE \"track01.bin\" BINARY\n" +
                        "  TRACK 01 MODE1/2352\n" +
                        "    INDEX 01 00:00:00\n" +
                        "FILE \"track02.bin\" BINARY\n" +
                        "  TRACK 02 AUDIO\n" +
                        "    INDEX 01 00:00:00\n",
                StandardCharsets.US_ASCII);
        return cue;
    }

    @Test
    void parsesTrackListCorrectly() throws IOException {
        Path cue = buildSingleFileSample();
        try (CueBinImage image = CueBinImage.load(cue)) {
            List<CueTrack> tracks = image.tracks();
            assertEquals(2, tracks.size());

            CueTrack track1 = tracks.get(0);
            assertEquals(1, track1.trackNumber());
            assertEquals(0, track1.startLba());
            assertEquals(2, track1.sectorCount());
            assertEquals(CueTrackMode.MODE1_2352, track1.mode());
            assertEquals("game.bin", track1.fileName());

            CueTrack track2 = tracks.get(1);
            assertEquals(2, track2.trackNumber());
            assertEquals(2, track2.startLba());
            assertEquals(1, track2.sectorCount());
            assertEquals(CueTrackMode.AUDIO, track2.mode());
        }
    }

    @Test
    void readsBothSectorsOfFirstTrackFromSharedFile() throws IOException {
        Path cue = buildSingleFileSample();
        try (CueBinImage image = CueBinImage.load(cue)) {
            byte[] buffer = new byte[2352];

            image.readSector(0, buffer);
            byte[] expectedFirst = new byte[2352];
            fillSector(expectedFirst, 0, 2352, (byte) 0xAA);
            assertArrayEquals(expectedFirst, buffer);

            image.readSector(1, buffer);
            byte[] expectedSecond = new byte[2352];
            fillSector(expectedSecond, 0, 2352, (byte) 0xBB);
            assertArrayEquals(expectedSecond, buffer);
        }
    }

    @Test
    void readsSecondTrackAtItsOwnStartLbaFromSharedFile() throws IOException {
        Path cue = buildSingleFileSample();
        try (CueBinImage image = CueBinImage.load(cue)) {
            byte[] buffer = new byte[2352];
            image.readSector(2, buffer); // track 2 starts at LBA 2

            byte[] expected = new byte[2352];
            fillSector(expected, 0, 2352, (byte) 0xCC);
            assertArrayEquals(expected, buffer);
        }
    }

    @Test
    void readsBothTracksWithOneFileEach() throws IOException {
        Path cue = buildSeparateFilesSample();
        try (CueBinImage image = CueBinImage.load(cue)) {
            byte[] buffer = new byte[2352];

            image.readSector(0, buffer); // track 1, sector 0
            byte[] expected1a = new byte[2352];
            fillSector(expected1a, 0, 2352, (byte) 0x11);
            assertArrayEquals(expected1a, buffer);

            image.readSector(1, buffer); // track 1, sector 1
            byte[] expected1b = new byte[2352];
            fillSector(expected1b, 0, 2352, (byte) 0x22);
            assertArrayEquals(expected1b, buffer);

            image.readSector(2, buffer); // track 2 starts at global LBA 2, its own file's offset 0
            byte[] expected2 = new byte[2352];
            fillSector(expected2, 0, 2352, (byte) 0x33);
            assertArrayEquals(expected2, buffer);
        }
    }

    @Test
    void lbaBeforeFirstTrackThrows() throws IOException {
        Path cue = buildSingleFileSample();
        try (CueBinImage image = CueBinImage.load(cue)) {
            assertThrows(IllegalArgumentException.class, () -> image.readSector(-1, new byte[2352]));
        }
    }

    @Test
    void missingBinFileThrowsClearError() throws IOException {
        Path cue = tempDir.resolve("missing-file.cue");
        Files.writeString(cue,
                "FILE \"does-not-exist.bin\" BINARY\n" +
                        "  TRACK 01 MODE1/2352\n" +
                        "    INDEX 01 00:00:00\n",
                StandardCharsets.US_ASCII);

        IOException thrown = assertThrows(IOException.class, () -> CueBinImage.load(cue));
        assertTrue(thrown.getMessage().contains("does-not-exist.bin"));
    }

    @Test
    void missingIndexOneThrows() throws IOException {
        Path cue = tempDir.resolve("no-index.cue");
        Files.write(tempDir.resolve("game.bin"), new byte[2352]);
        Files.writeString(cue,
                "FILE \"game.bin\" BINARY\n" +
                        "  TRACK 01 MODE1/2352\n", // no INDEX 01 line at all
                StandardCharsets.US_ASCII);

        assertThrows(IOException.class, () -> CueBinImage.load(cue));
    }

    @Test
    void unsupportedFileTypeThrows() throws IOException {
        Path cue = tempDir.resolve("wave-type.cue");
        Files.write(tempDir.resolve("audio.wav"), new byte[2352]);
        Files.writeString(cue,
                "FILE \"audio.wav\" WAVE\n" +
                        "  TRACK 01 AUDIO\n" +
                        "    INDEX 01 00:00:00\n",
                StandardCharsets.US_ASCII);

        assertThrows(IOException.class, () -> CueBinImage.load(cue));
    }

    @Test
    void malformedTrackModeThrows() throws IOException {
        Path cue = tempDir.resolve("bad-mode.cue");
        Files.write(tempDir.resolve("game.bin"), new byte[2352]);
        Files.writeString(cue,
                "FILE \"game.bin\" BINARY\n" +
                        "  TRACK 01 NOT_A_REAL_MODE\n" +
                        "    INDEX 01 00:00:00\n",
                StandardCharsets.US_ASCII);

        assertThrows(IOException.class, () -> CueBinImage.load(cue));
    }

    @Test
    void nonWholeSectorFileSizeThrows() throws IOException {
        Path cue = tempDir.resolve("truncated.cue");
        // 2352-byte sectors, but the file is a few bytes short of a whole sector.
        Files.write(tempDir.resolve("game.bin"), new byte[2352 + 100]);
        Files.writeString(cue,
                "FILE \"game.bin\" BINARY\n" +
                        "  TRACK 01 MODE1/2352\n" +
                        "    INDEX 01 00:00:00\n",
                StandardCharsets.US_ASCII);

        assertThrows(IOException.class, () -> CueBinImage.load(cue));
    }
}
