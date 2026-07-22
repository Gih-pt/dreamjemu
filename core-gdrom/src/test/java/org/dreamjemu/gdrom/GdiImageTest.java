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

class GdiImageTest {

    @TempDir
    Path tempDir;

    /**
     * Builds a small, real two-track GDI image on disk: a 2-sector data
     * track followed by a 1-sector audio track, each with distinct,
     * recognizable sector content, so reads can be verified byte-for-byte.
     */
    private Path buildSampleGdi() throws IOException {
        int sectorSize = 2352;

        byte[] track1 = new byte[sectorSize * 2];
        fillSector(track1, 0, sectorSize, (byte) 0xAA);   // track 1, LBA 0
        fillSector(track1, sectorSize, sectorSize, (byte) 0xBB); // track 1, LBA 1

        byte[] track2 = new byte[sectorSize * 1];
        fillSector(track2, 0, sectorSize, (byte) 0xCC);   // track 2, LBA 750

        Files.write(tempDir.resolve("track01.bin"), track1);
        Files.write(tempDir.resolve("track02.raw"), track2);

        Path gdi = tempDir.resolve("game.gdi");
        Files.writeString(gdi,
                "2\n" +
                        "1 0 4 2352 track01.bin 0\n" +
                        "2 750 0 2352 track02.raw 0\n",
                StandardCharsets.US_ASCII);
        return gdi;
    }

    private static void fillSector(byte[] array, int offset, int length, byte value) {
        for (int i = 0; i < length; i++) {
            array[offset + i] = value;
        }
    }

    @Test
    void parsesTrackListCorrectly() throws IOException {
        Path gdi = buildSampleGdi();
        try (GdiImage image = GdiImage.load(gdi)) {
            List<GdiTrack> tracks = image.tracks();
            assertEquals(2, tracks.size());

            GdiTrack track1 = tracks.get(0);
            assertEquals(1, track1.trackNumber());
            assertEquals(0, track1.startLba());
            assertEquals(GdiTrackType.DATA, track1.type());
            assertEquals(2352, track1.sectorSize());
            assertEquals("track01.bin", track1.fileName());

            GdiTrack track2 = tracks.get(1);
            assertEquals(2, track2.trackNumber());
            assertEquals(750, track2.startLba());
            assertEquals(GdiTrackType.AUDIO, track2.type());
        }
    }

    @Test
    void readsFirstSectorOfFirstTrack() throws IOException {
        Path gdi = buildSampleGdi();
        try (GdiImage image = GdiImage.load(gdi)) {
            byte[] buffer = new byte[2352];
            image.readSector(0, buffer);

            byte[] expected = new byte[2352];
            fillSector(expected, 0, 2352, (byte) 0xAA);
            assertArrayEquals(expected, buffer);
        }
    }

    @Test
    void readsSecondSectorOfFirstTrack() throws IOException {
        Path gdi = buildSampleGdi();
        try (GdiImage image = GdiImage.load(gdi)) {
            byte[] buffer = new byte[2352];
            image.readSector(1, buffer);

            byte[] expected = new byte[2352];
            fillSector(expected, 0, 2352, (byte) 0xBB);
            assertArrayEquals(expected, buffer);
        }
    }

    @Test
    void readsSectorFromSecondTrackAtItsOwnStartLba() throws IOException {
        Path gdi = buildSampleGdi();
        try (GdiImage image = GdiImage.load(gdi)) {
            byte[] buffer = new byte[2352];
            image.readSector(750, buffer); // track 2 starts at LBA 750

            byte[] expected = new byte[2352];
            fillSector(expected, 0, 2352, (byte) 0xCC);
            assertArrayEquals(expected, buffer);
        }
    }

    @Test
    void lbaBeforeFirstTrackThrows() throws IOException {
        Path gdi = buildSampleGdi();
        try (GdiImage image = GdiImage.load(gdi)) {
            assertThrows(IllegalArgumentException.class, () -> image.readSector(-1, new byte[2352]));
        }
    }

    @Test
    void mismatchedTrackCountThrows() throws IOException {
        Path gdi = tempDir.resolve("bad-count.gdi");
        Files.writeString(gdi,
                "3\n" + // declares 3 tracks
                        "1 0 4 2352 track01.bin 0\n", // but only provides 1
                StandardCharsets.US_ASCII);

        assertThrows(IOException.class, () -> GdiImage.load(gdi));
    }

    @Test
    void missingTrackFileThrowsClearError() throws IOException {
        Path gdi = tempDir.resolve("missing-file.gdi");
        Files.writeString(gdi,
                "1\n1 0 4 2352 does-not-exist.bin 0\n",
                StandardCharsets.US_ASCII);

        try (GdiImage image = GdiImage.load(gdi)) {
            IOException thrown = assertThrows(IOException.class, () -> image.readSector(0, new byte[2352]));
            assertEquals(true, thrown.getMessage().contains("does-not-exist.bin"));
        }
    }
}
