package org.dreamjemu.gdrom;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parses a DiscJuggler CDI image and provides sector-level reads, following
 * the same overall shape as {@link GdiImage} and {@link CueBinImage}: parse
 * the track layout once in {@link #load}, then read whatever sector is
 * asked for via {@link #readSector}. See /docs/ROADMAP.md — "CDI/CHD reading".
 *
 * <p>Unlike GDI/CUE, a CDI image is a single monolithic file: every
 * session/track's data, plus a binary header describing them, all live in
 * the one .cdi file. The header isn't at a fixed location — a small trailer
 * at the very end of the file gives its version and location.
 *
 * <p>This class's parsing logic (field order, sizes, and the fixed 20-byte
 * "track start" marker) is derived from the structural facts documented by
 * the open-source DreamShell project's DiscJuggler CDI reader
 * ({@code include/isofs/cdi.h} and {@code modules/isofs/cdi.c}, by SWAT /
 * dc-swat.ru) — a binary container layout, not creative expression — and is
 * implemented here independently, in this project's own style; no code was
 * copied from that project. Cross-checked against DreamShell's declared
 * version constants ({@code CDI_V2_ID}/{@code CDI_V3_ID}/{@code CDI_V35_ID}
 * = 0x80000004/0x80000005/0x80000006).
 *
 * <p>This class only reads whatever .cdi file the user already has locally;
 * it never requires or reads any original console/BIOS file.
 */
public final class CdiImage implements AutoCloseable {

    /** DiscJuggler CDI v2 — header_offset is absolute from the start of the file. */
    private static final long VERSION_2 = 0x80000004L;
    /** DiscJuggler CDI v3 — same header addressing as v2, with extra per-track fields. */
    private static final long VERSION_3 = 0x80000005L;
    /** DiscJuggler CDI v3.5 — header_offset is instead measured back from the end of the file. */
    private static final long VERSION_3_5 = 0x80000006L;

    private static final int MAX_SESSIONS = 2; // Dreamcast CDI images never have more than 2.
    private static final int MAX_TRACKS = 99;

    /** Fixed 20-byte marker that must precede each track's data in the header. */
    private static final byte[] TRACK_START_MARKER = {
            0x00, 0x00, 0x01, 0x00, 0x00, 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            0x00, 0x00, 0x01, 0x00, 0x00, 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
    };

    private final RandomAccessFile file;
    private final List<CdiTrack> tracksByLba; // sorted ascending by startLba

    private CdiImage(RandomAccessFile file, List<CdiTrack> tracksByLba) {
        this.file = file;
        this.tracksByLba = tracksByLba;
    }

    /**
     * Parses the given .cdi file's trailer and header, resolving every
     * session's tracks. Keeps the file open for subsequent {@link #readSector}
     * calls — call {@link #close()} when done.
     *
     * @throws IOException if the file is too short, its trailer/header don't
     *                      structurally check out, or its version isn't recognized
     */
    public static CdiImage load(Path cdiFile) throws IOException {
        RandomAccessFile file = new RandomAccessFile(cdiFile.toFile(), "r");
        try {
            long fileLength = file.length();
            if (fileLength < 8) {
                throw new IOException("CDI file " + cdiFile + " is too short to contain a trailer");
            }

            file.seek(fileLength - 8);
            long version = readUInt32LE(file);
            long headerOffset = readUInt32LE(file);

            if (version != VERSION_2 && version != VERSION_3 && version != VERSION_3_5) {
                throw new IOException("CDI file " + cdiFile + " has an unrecognized version marker: 0x" +
                        Long.toHexString(version));
            }
            if (headerOffset == 0 || headerOffset >= fileLength) {
                throw new IOException("CDI file " + cdiFile + " has an invalid header offset: " + headerOffset);
            }

            long headerPosition = (version == VERSION_3_5) ? (fileLength - headerOffset) : headerOffset;
            if (headerPosition < 0 || headerPosition >= fileLength) {
                throw new IOException("CDI file " + cdiFile + " has a header position outside the file: " +
                        headerPosition);
            }

            file.seek(headerPosition);
            List<CdiTrack> tracks = parseSessions(file, version, cdiFile);
            tracks.sort(java.util.Comparator.comparingLong(CdiTrack::startLba));
            return new CdiImage(file, Collections.unmodifiableList(tracks));
        } catch (IOException | RuntimeException e) {
            file.close();
            throw e;
        }
    }

    private static List<CdiTrack> parseSessions(RandomAccessFile file, long version, Path cdiFile) throws IOException {
        int sessionCount = readUInt16LE(file);
        if (sessionCount < 1 || sessionCount > MAX_SESSIONS) {
            throw new IOException("CDI file " + cdiFile + " has an invalid session count: " + sessionCount);
        }

        List<CdiTrack> tracks = new ArrayList<>();
        long runningPosition = 0;
        int globalTrackNumber = 0;

        for (int session = 1; session <= sessionCount; session++) {
            int trackCount = readUInt16LE(file);
            if (trackCount < 1 || trackCount > MAX_TRACKS || globalTrackNumber + trackCount > MAX_TRACKS) {
                throw new IOException("CDI file " + cdiFile + " has an invalid track count in session " +
                        session + ": " + trackCount);
            }

            for (int t = 0; t < trackCount; t++) {
                long newFmt = readUInt32LE(file);
                if (newFmt != 0) {
                    skip(file, 8); // additional data present (DiscJuggler 3.00.780+)
                }

                byte[] marker = new byte[20];
                file.readFully(marker);
                if (!java.util.Arrays.equals(marker, TRACK_START_MARKER)) {
                    throw new IOException("CDI file " + cdiFile + " is missing the expected track-start marker " +
                            "for track " + (globalTrackNumber + 1) + " — corrupt or unsupported CDI image");
                }

                skip(file, 4);
                int fileNameLength = readUInt8(file);
                skip(file, fileNameLength); // the per-track file name, unused: all data lives in this one .cdi file
                skip(file, 19);

                long secondMarker = readUInt32LE(file);
                skip(file, secondMarker == 0x80000000L ? 10 : 2);

                long pregapLength = readUInt32LE(file);
                long length = readUInt32LE(file);
                skip(file, 6);
                long modeCode = readUInt32LE(file);
                skip(file, 12);
                long startLba = readUInt32LE(file);
                long totalLength = readUInt32LE(file);
                skip(file, 16);
                long sectorSizeCode = readUInt32LE(file);
                skip(file, 29);

                if (version != VERSION_2) {
                    skip(file, 5);
                    long extMarker = readUInt32LE(file);
                    if (extMarker == 0xFFFFFFFFL) {
                        skip(file, 78);
                    }
                }

                CdiTrackMode mode;
                int sectorSize;
                try {
                    mode = CdiTrackMode.fromCode(modeCode);
                    sectorSize = mode.sectorSizeFor(sectorSizeCode);
                } catch (IllegalArgumentException e) {
                    throw new IOException("CDI file " + cdiFile + " has an invalid mode/sector-size for track " +
                            (globalTrackNumber + 1) + ": " + e.getMessage(), e);
                }

                long fileOffset = runningPosition + pregapLength * sectorSize;
                globalTrackNumber++;
                tracks.add(new CdiTrack(session, globalTrackNumber, startLba, length, mode, sectorSize, fileOffset));

                runningPosition += totalLength * sectorSize;
            }

            skip(file, 12);
            if (version != VERSION_2) {
                skip(file, 1);
            }
        }

        return tracks;
    }

    // ---- Reading -------------------------------------------------------

    /** Returns all tracks across every session, sorted by ascending start LBA. */
    public List<CdiTrack> tracks() {
        return tracksByLba;
    }

    /**
     * Finds the track that contains the given LBA (i.e. the last track whose
     * start LBA is less than or equal to it), matching the same convention
     * used by {@link GdiImage#trackContainingLba} and
     * {@link CueBinImage#trackContainingLba}.
     *
     * @throws IllegalArgumentException if the LBA is before the first track
     */
    public CdiTrack trackContainingLba(long lba) {
        CdiTrack found = null;
        for (CdiTrack track : tracksByLba) {
            if (lba >= track.startLba()) {
                found = track;
            } else {
                break;
            }
        }
        if (found == null) {
            throw new IllegalArgumentException("LBA " + lba + " is before the first track in this image");
        }
        return found;
    }

    /**
     * Reads one sector's worth of data at the given LBA into {@code dest}.
     *
     * @param dest buffer to read into; must be at least as large as the containing track's sector size
     * @throws IOException if the underlying file can't be read
     */
    public void readSector(long lba, byte[] dest) throws IOException {
        CdiTrack track = trackContainingLba(lba);

        if (dest.length < track.sectorSize()) {
            throw new IllegalArgumentException("Destination buffer (" + dest.length +
                    " bytes) is smaller than track " + track.trackNumber() +
                    "'s sector size (" + track.sectorSize() + " bytes)");
        }

        long sectorIndexInTrack = lba - track.startLba();
        long filePosition = track.fileOffset() + sectorIndexInTrack * track.sectorSize();
        file.seek(filePosition);
        file.readFully(dest, 0, track.sectorSize());
    }

    @Override
    public void close() throws IOException {
        file.close();
    }

    // ---- Little-endian binary reading helpers --------------------------

    private static int readUInt8(RandomAccessFile file) throws IOException {
        int value = file.read();
        if (value < 0) {
            throw new EOFException();
        }
        return value;
    }

    private static int readUInt16LE(RandomAccessFile file) throws IOException {
        byte[] b = new byte[2];
        file.readFully(b);
        return (b[0] & 0xFF) | ((b[1] & 0xFF) << 8);
    }

    private static long readUInt32LE(RandomAccessFile file) throws IOException {
        byte[] b = new byte[4];
        file.readFully(b);
        return (b[0] & 0xFFL) | ((b[1] & 0xFFL) << 8) | ((b[2] & 0xFFL) << 16) | ((b[3] & 0xFFL) << 24);
    }

    private static void skip(RandomAccessFile file, int byteCount) throws IOException {
        file.seek(file.getFilePointer() + byteCount);
    }
}
