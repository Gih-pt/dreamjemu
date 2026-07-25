package org.dreamjemu.gdrom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Builds small, synthetic CDI (DiscJuggler) files byte-by-byte matching the
 * documented structure (see {@link CdiImage}'s class Javadoc for the source),
 * rather than using any real game image, and checks that {@link CdiImage}
 * parses and reads them correctly.
 */
class CdiImageTest {

    @TempDir
    Path tempDir;

    private static final long VERSION_3 = 0x80000005L; // most common real-world CDI version
    private static final byte[] TRACK_START_MARKER = {
            0x00, 0x00, 0x01, 0x00, 0x00, 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            0x00, 0x00, 0x01, 0x00, 0x00, 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
    };

    private static void writeLE16(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
    }

    private static void writeLE32(ByteArrayOutputStream out, long value) {
        out.write((int) (value & 0xFF));
        out.write((int) ((value >> 8) & 0xFF));
        out.write((int) ((value >> 16) & 0xFF));
        out.write((int) ((value >> 24) & 0xFF));
    }

    private static void writeFiller(ByteArrayOutputStream out, int count) {
        out.write(new byte[count], 0, count);
    }

    /** Writes one track record exactly as {@link CdiImage} expects to parse it (CDI v3 layout). */
    private static void writeTrackRecord(ByteArrayOutputStream out, long modeCode, long sectorSizeCode,
                                          long pregapLength, long length, long startLba) {
        writeLE32(out, 0); // newFmt: no additional 3.00.780+ data
        out.write(TRACK_START_MARKER, 0, TRACK_START_MARKER.length);
        writeFiller(out, 4);
        out.write(0); // fileNameLength = 0 (no filename bytes follow)
        writeFiller(out, 19);
        writeLE32(out, 0); // secondMarker != 0x80000000 -> 2 filler bytes follow
        writeFiller(out, 2);
        writeLE32(out, pregapLength);
        writeLE32(out, length);
        writeFiller(out, 6);
        writeLE32(out, modeCode);
        writeFiller(out, 12);
        writeLE32(out, startLba);
        writeLE32(out, pregapLength + length); // totalLength
        writeFiller(out, 16);
        writeLE32(out, sectorSizeCode);
        writeFiller(out, 29);
        // version != V2: 5 filler bytes, then an extMarker that isn't 0xFFFFFFFF (no extra 78 bytes)
        writeFiller(out, 5);
        writeLE32(out, 0);
    }

    private static void fillSector(byte[] array, int offset, int length, byte value) {
        for (int i = 0; i < length; i++) {
            array[offset + i] = value;
        }
    }

    /** One session, one MODE1/2048-equivalent data track: 3 sectors, no pregap, LBA 0. */
    private Path buildSingleTrackCdi() throws IOException {
        int sectorSize = 2048;
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        byte[] sector0 = new byte[sectorSize];
        byte[] sector1 = new byte[sectorSize];
        byte[] sector2 = new byte[sectorSize];
        fillSector(sector0, 0, sectorSize, (byte) 0xAA);
        fillSector(sector1, 0, sectorSize, (byte) 0xBB);
        fillSector(sector2, 0, sectorSize, (byte) 0xCC);
        data.write(sector0, 0, sectorSize);
        data.write(sector1, 0, sectorSize);
        data.write(sector2, 0, sectorSize);

        ByteArrayOutputStream header = new ByteArrayOutputStream();
        writeLE16(header, 1); // sessionCount
        writeLE16(header, 1); // trackCount (session 1)
        writeTrackRecord(header, 1 /* DATA */, 0 /* -> 2048 */, 0, 3, 0);
        writeFiller(header, 12);
        writeFiller(header, 1); // version != V2 -> 1 extra session-trailer byte

        long headerOffset = data.size();
        ByteArrayOutputStream file = new ByteArrayOutputStream();
        file.write(data.toByteArray());
        file.write(header.toByteArray());
        writeLE32(file, VERSION_3);
        writeLE32(file, headerOffset);

        Path path = tempDir.resolve("single-track.cdi");
        Files.write(path, file.toByteArray());
        return path;
    }

    /** Two sessions: session 1 has one CDDA (audio) track, session 2 has one DATA track at a distinct LBA. */
    private Path buildTwoSessionCdi() throws IOException {
        int audioSectorSize = 2352;
        int dataSectorSize = 2048;

        ByteArrayOutputStream data = new ByteArrayOutputStream();
        byte[] audio0 = new byte[audioSectorSize];
        byte[] audio1 = new byte[audioSectorSize];
        fillSector(audio0, 0, audioSectorSize, (byte) 0x11);
        fillSector(audio1, 0, audioSectorSize, (byte) 0x22);
        data.write(audio0, 0, audioSectorSize);
        data.write(audio1, 0, audioSectorSize);

        byte[] dataSector0 = new byte[dataSectorSize];
        byte[] dataSector1 = new byte[dataSectorSize];
        fillSector(dataSector0, 0, dataSectorSize, (byte) 0x33);
        fillSector(dataSector1, 0, dataSectorSize, (byte) 0x44);
        data.write(dataSector0, 0, dataSectorSize);
        data.write(dataSector1, 0, dataSectorSize);

        ByteArrayOutputStream header = new ByteArrayOutputStream();
        writeLE16(header, 2); // sessionCount

        writeLE16(header, 1); // session 1: trackCount
        writeTrackRecord(header, 0 /* CDDA */, 2 /* -> 2352 */, 0, 2, 0);
        writeFiller(header, 12);
        writeFiller(header, 1);

        writeLE16(header, 1); // session 2: trackCount
        writeTrackRecord(header, 1 /* DATA */, 0 /* -> 2048 */, 0, 2, 100);
        writeFiller(header, 12);
        writeFiller(header, 1);

        long headerOffset = data.size();
        ByteArrayOutputStream file = new ByteArrayOutputStream();
        file.write(data.toByteArray());
        file.write(header.toByteArray());
        writeLE32(file, VERSION_3);
        writeLE32(file, headerOffset);

        Path path = tempDir.resolve("two-session.cdi");
        Files.write(path, file.toByteArray());
        return path;
    }

    @Test
    void parsesSingleSessionSingleTrackCorrectly() throws IOException {
        Path cdi = buildSingleTrackCdi();
        try (CdiImage image = CdiImage.load(cdi)) {
            List<CdiTrack> tracks = image.tracks();
            assertEquals(1, tracks.size());

            CdiTrack track = tracks.get(0);
            assertEquals(1, track.sessionNumber());
            assertEquals(1, track.trackNumber());
            assertEquals(0, track.startLba());
            assertEquals(3, track.sectorCount());
            assertEquals(CdiTrackMode.DATA, track.mode());
            assertEquals(2048, track.sectorSize());
        }
    }

    @Test
    void readsAllSectorsOfSingleTrack() throws IOException {
        Path cdi = buildSingleTrackCdi();
        try (CdiImage image = CdiImage.load(cdi)) {
            byte[] buffer = new byte[2048];

            image.readSector(0, buffer);
            byte[] expected0 = new byte[2048];
            fillSector(expected0, 0, 2048, (byte) 0xAA);
            assertArrayEquals(expected0, buffer);

            image.readSector(1, buffer);
            byte[] expected1 = new byte[2048];
            fillSector(expected1, 0, 2048, (byte) 0xBB);
            assertArrayEquals(expected1, buffer);

            image.readSector(2, buffer);
            byte[] expected2 = new byte[2048];
            fillSector(expected2, 0, 2048, (byte) 0xCC);
            assertArrayEquals(expected2, buffer);
        }
    }

    @Test
    void parsesTwoSessionsWithDistinctTrackLbas() throws IOException {
        Path cdi = buildTwoSessionCdi();
        try (CdiImage image = CdiImage.load(cdi)) {
            List<CdiTrack> tracks = image.tracks();
            assertEquals(2, tracks.size());

            CdiTrack audioTrack = tracks.get(0);
            assertEquals(1, audioTrack.sessionNumber());
            assertEquals(1, audioTrack.trackNumber());
            assertEquals(0, audioTrack.startLba());
            assertEquals(CdiTrackMode.CDDA, audioTrack.mode());
            assertEquals(2352, audioTrack.sectorSize());

            CdiTrack dataTrack = tracks.get(1);
            assertEquals(2, dataTrack.sessionNumber());
            assertEquals(2, dataTrack.trackNumber());
            assertEquals(100, dataTrack.startLba());
            assertEquals(CdiTrackMode.DATA, dataTrack.mode());
            assertEquals(2048, dataTrack.sectorSize());
        }
    }

    @Test
    void readsAcrossBothSessions() throws IOException {
        Path cdi = buildTwoSessionCdi();
        try (CdiImage image = CdiImage.load(cdi)) {
            byte[] audioBuffer = new byte[2352];
            image.readSector(0, audioBuffer);
            byte[] expectedAudio0 = new byte[2352];
            fillSector(expectedAudio0, 0, 2352, (byte) 0x11);
            assertArrayEquals(expectedAudio0, audioBuffer);

            image.readSector(1, audioBuffer);
            byte[] expectedAudio1 = new byte[2352];
            fillSector(expectedAudio1, 0, 2352, (byte) 0x22);
            assertArrayEquals(expectedAudio1, audioBuffer);

            byte[] dataBuffer = new byte[2048];
            image.readSector(100, dataBuffer);
            byte[] expectedData0 = new byte[2048];
            fillSector(expectedData0, 0, 2048, (byte) 0x33);
            assertArrayEquals(expectedData0, dataBuffer);

            image.readSector(101, dataBuffer);
            byte[] expectedData1 = new byte[2048];
            fillSector(expectedData1, 0, 2048, (byte) 0x44);
            assertArrayEquals(expectedData1, dataBuffer);
        }
    }

    @Test
    void lbaBeforeFirstTrackThrows() throws IOException {
        Path cdi = buildSingleTrackCdi();
        try (CdiImage image = CdiImage.load(cdi)) {
            assertThrows(IllegalArgumentException.class, () -> image.readSector(-1, new byte[2048]));
        }
    }

    @Test
    void fileTooShortForTrailerThrows() throws IOException {
        Path path = tempDir.resolve("too-short.cdi");
        Files.write(path, new byte[4]); // less than the 8-byte trailer
        assertThrows(IOException.class, () -> CdiImage.load(path));
    }

    @Test
    void unrecognizedVersionMarkerThrows() throws IOException {
        ByteArrayOutputStream file = new ByteArrayOutputStream();
        writeFiller(file, 64);
        writeLE32(file, 0x12345678L); // not a known CDI version
        writeLE32(file, 32);

        Path path = tempDir.resolve("bad-version.cdi");
        Files.write(path, file.toByteArray());

        assertThrows(IOException.class, () -> CdiImage.load(path));
    }

    @Test
    void invalidHeaderOffsetThrows() throws IOException {
        ByteArrayOutputStream file = new ByteArrayOutputStream();
        writeFiller(file, 64);
        writeLE32(file, VERSION_3);
        writeLE32(file, 0); // header_offset == 0 is invalid

        Path path = tempDir.resolve("bad-offset.cdi");
        Files.write(path, file.toByteArray());

        assertThrows(IOException.class, () -> CdiImage.load(path));
    }

    @Test
    void missingTrackStartMarkerThrows() throws IOException {
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        writeLE16(header, 1); // sessionCount
        writeLE16(header, 1); // trackCount
        writeLE32(header, 0); // newFmt
        writeFiller(header, 20); // wrong bytes where the 20-byte marker should be

        int headerOffset = 8; // header_offset can't be 0, so prepend a few filler "track data" bytes first
        ByteArrayOutputStream file = new ByteArrayOutputStream();
        writeFiller(file, headerOffset);
        file.write(header.toByteArray());
        writeLE32(file, VERSION_3);
        writeLE32(file, headerOffset);

        Path path = tempDir.resolve("bad-marker.cdi");
        Files.write(path, file.toByteArray());

        assertThrows(IOException.class, () -> CdiImage.load(path));
    }

    @Test
    void invalidModeSectorSizeCombinationThrows() throws IOException {
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        writeLE16(header, 1); // sessionCount
        writeLE16(header, 1); // trackCount
        // DATA (mode 1) only accepts sector-size code 0 (2048) — code 1 is invalid for it.
        writeTrackRecord(header, 1, 1, 0, 1, 0);
        writeFiller(header, 12);
        writeFiller(header, 1);

        long headerOffset = 4; // arbitrary small offset; no real track data needed before it for this test
        ByteArrayOutputStream file = new ByteArrayOutputStream();
        writeFiller(file, (int) headerOffset);
        file.write(header.toByteArray());
        writeLE32(file, VERSION_3);
        writeLE32(file, headerOffset);

        Path path = tempDir.resolve("bad-mode.cdi");
        Files.write(path, file.toByteArray());

        assertThrows(IOException.class, () -> CdiImage.load(path));
    }
}
