package org.dreamjemu.gdrom;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a CUE sheet plus its referenced .bin file(s) and provides
 * sector-level reads, following the same pattern as {@link GdiImage}: parse
 * the text index once, then read whatever sector is asked for lazily,
 * resolving track data files relative to the .cue file's own directory. See
 * /docs/ROADMAP.md — "CDI/CHD/CUE-BIN reading".
 *
 * A CUE sheet is a plain-text index describing one or more "FILE" sections,
 * each containing one or more "TRACK" entries, each with an "INDEX 01"
 * marking where the track's actual data starts (in {@code MM:SS:FF} format,
 * relative to the start of its FILE — 75 frames per second, and in this
 * context a "frame" numerically is one sector, not a specific byte count).
 * Unlike a .gdi file, a CUE sheet doesn't declare each track's global LBA or
 * sector count directly — this class derives both: a track's sector count
 * comes from the gap to the next track's INDEX 01 in the same file (or the
 * remaining file size, for the last track in a file), and its global LBA is
 * the running total of every earlier track's sector count, matching the
 * whole-disc LBA numbering already used by {@link GdiImage} (track 1 starts
 * at LBA 0).
 *
 * This class only reads whatever files the user already has locally; it
 * never requires or reads any original console/BIOS file.
 */
public final class CueBinImage implements AutoCloseable {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"([^\"]*)\"|(\\S+)");
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("(\\d+):(\\d{2}):(\\d{2})");
    private static final int FRAMES_PER_SECOND = 75;

    private final Path baseDir;
    private final List<CueTrack> tracksByLba; // sorted ascending by startLba
    private final Map<String, RandomAccessFile> openFilesByName = new HashMap<>();

    private CueBinImage(Path baseDir, List<CueTrack> tracksByLba) {
        this.baseDir = baseDir;
        this.tracksByLba = tracksByLba;
    }

    /**
     * Parses the given .cue file and resolves its track layout, including
     * computing each track's sector count and global LBA. Does not open any
     * .bin file yet — those are opened lazily on first read, in
     * {@link #readSector}. Referenced .bin files must already exist at parse
     * time, though, since their sizes are needed to compute the last track's
     * sector count in each file.
     *
     * @throws IOException if the .cue file can't be read, its structure is invalid,
     *                      it references an unsupported FILE type, or a referenced
     *                      .bin file is missing
     */
    public static CueBinImage load(Path cueFile) throws IOException {
        List<RawTrack> rawTracks = parseCueSheet(cueFile);
        Path baseDir = cueFile.toAbsolutePath().getParent();
        List<CueTrack> tracks = resolveTracks(rawTracks, baseDir, cueFile);
        return new CueBinImage(baseDir, Collections.unmodifiableList(tracks));
    }

    // ---- Parsing -----------------------------------------------------

    /** A track as directly declared in the CUE sheet, before LBA/sector-count resolution. */
    private record RawTrack(int trackNumber, String fileName, CueTrackMode mode, long indexFrameOffset) {
    }

    private static List<RawTrack> parseCueSheet(Path cueFile) throws IOException {
        List<String> lines = Files.readAllLines(cueFile);

        List<RawTrack> rawTracks = new ArrayList<>();

        String currentFileName = null;
        int trackNumberInProgress = -1;
        String trackFileNameInProgress = null; // fileName active when this track's TRACK line was seen
        CueTrackMode modeInProgress = null;
        long indexFrameOffset = -1;

        for (String rawLine : lines) {
            String line = rawLine.strip();
            if (line.isEmpty()) {
                continue;
            }
            List<String> tokens = tokenize(line);
            String keyword = tokens.get(0).toUpperCase(java.util.Locale.ROOT);

            switch (keyword) {
                case "FILE" -> {
                    if (trackNumberInProgress != -1 && indexFrameOffset < 0) {
                        throw new IOException("CUE file " + cueFile + " has track " + trackNumberInProgress +
                                " with no INDEX 01 before the next FILE line");
                    }
                    if (tokens.size() < 3) {
                        throw new IOException("Malformed FILE line in " + cueFile + ": \"" + line + "\"");
                    }
                    currentFileName = tokens.get(1);
                    String fileType = tokens.get(2);
                    if (!fileType.equalsIgnoreCase("BINARY")) {
                        throw new IOException("CUE file " + cueFile + " references unsupported FILE type \"" +
                                fileType + "\" (only BINARY is supported)");
                    }
                }
                case "TRACK" -> {
                    if (currentFileName == null) {
                        throw new IOException("CUE file " + cueFile + " has a TRACK line before any FILE line: \"" +
                                line + "\"");
                    }
                    if (trackNumberInProgress != -1) {
                        if (indexFrameOffset < 0) {
                            throw new IOException("CUE file " + cueFile + " has track " + trackNumberInProgress +
                                    " with no INDEX 01");
                        }
                        // Use the fileName captured when THIS track's TRACK line was seen, not
                        // currentFileName — a later FILE line (for the next track) may already
                        // have changed it by the time we get here.
                        rawTracks.add(new RawTrack(trackNumberInProgress, trackFileNameInProgress, modeInProgress, indexFrameOffset));
                    }
                    if (tokens.size() < 3) {
                        throw new IOException("Malformed TRACK line in " + cueFile + ": \"" + line + "\"");
                    }
                    try {
                        trackNumberInProgress = Integer.parseInt(tokens.get(1));
                    } catch (NumberFormatException e) {
                        throw new IOException("Malformed TRACK line in " + cueFile + ": \"" + line + "\"", e);
                    }
                    try {
                        modeInProgress = CueTrackMode.fromToken(tokens.get(2));
                    } catch (IllegalArgumentException e) {
                        throw new IOException("Malformed TRACK line in " + cueFile + ": \"" + line + "\"", e);
                    }
                    trackFileNameInProgress = currentFileName;
                    indexFrameOffset = -1;
                }
                case "INDEX" -> {
                    if (trackNumberInProgress == -1) {
                        throw new IOException("CUE file " + cueFile + " has an INDEX line before any TRACK line: \"" +
                                line + "\"");
                    }
                    if (tokens.size() < 3) {
                        throw new IOException("Malformed INDEX line in " + cueFile + ": \"" + line + "\"");
                    }
                    int indexNumber;
                    try {
                        indexNumber = Integer.parseInt(tokens.get(1));
                    } catch (NumberFormatException e) {
                        throw new IOException("Malformed INDEX line in " + cueFile + ": \"" + line + "\"", e);
                    }
                    if (indexNumber == 1) {
                        indexFrameOffset = parseTimestamp(tokens.get(2), cueFile, line);
                    }
                    // INDEX 00 (pregap) and sub-indexes (02-99) don't affect sector-level reads here.
                }
                default -> {
                    // REM, TITLE, PERFORMER, CATALOG, FLAGS, PREGAP, POSTGAP, CDTEXTFILE, etc. — not
                    // needed for sector reading, so intentionally ignored.
                }
            }
        }

        if (trackNumberInProgress != -1) {
            if (indexFrameOffset < 0) {
                throw new IOException("CUE file " + cueFile + " has track " + trackNumberInProgress +
                        " with no INDEX 01");
            }
            rawTracks.add(new RawTrack(trackNumberInProgress, trackFileNameInProgress, modeInProgress, indexFrameOffset));
        }

        if (rawTracks.isEmpty()) {
            throw new IOException("CUE file " + cueFile + " declares no tracks");
        }

        return rawTracks;
    }

    private static List<String> tokenize(String line) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(line);
        while (matcher.find()) {
            tokens.add(matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
        }
        return tokens;
    }

    /**
     * Parses a CUE sheet {@code MM:SS:FF} timestamp into a frame count
     * (75 frames per second). Per the CUE sheet format, this frame count is
     * relative to the start of the current FILE and, in this context, is
     * numerically the same as a sector index within that file — it is
     * <b>not</b> offset by the 150-sector (2-second) lead-in adjustment used
     * for whole-disc absolute MSF addressing, since that offset only applies
     * to the disc's own table of contents, not to file-relative CUE indexes.
     */
    private static long parseTimestamp(String value, Path cueFile, String line) throws IOException {
        Matcher matcher = TIMESTAMP_PATTERN.matcher(value.strip());
        if (!matcher.matches()) {
            throw new IOException("Malformed INDEX timestamp in " + cueFile + ": \"" + line + "\"");
        }
        try {
            long minutes = Long.parseLong(matcher.group(1));
            long seconds = Long.parseLong(matcher.group(2));
            long frames = Long.parseLong(matcher.group(3));
            return (minutes * 60 + seconds) * FRAMES_PER_SECOND + frames;
        } catch (NumberFormatException e) {
            throw new IOException("Malformed INDEX timestamp in " + cueFile + ": \"" + line + "\"", e);
        }
    }

    // ---- LBA / sector-count resolution --------------------------------

    private static List<CueTrack> resolveTracks(List<RawTrack> rawTracks, Path baseDir, Path cueFile) throws IOException {
        List<CueTrack> resolved = new ArrayList<>(rawTracks.size());
        long runningLba = 0;
        int expectedTrackNumber = -1;

        for (int i = 0; i < rawTracks.size(); i++) {
            RawTrack track = rawTracks.get(i);

            if (expectedTrackNumber != -1 && track.trackNumber() <= expectedTrackNumber) {
                throw new IOException("CUE file " + cueFile + " has out-of-order track numbers (expected > " +
                        expectedTrackNumber + ", got " + track.trackNumber() + ")");
            }
            expectedTrackNumber = track.trackNumber();

            long fileOffsetBytes = track.indexFrameOffset() * track.mode().sectorSize();
            long sectorCount = resolveSectorCount(rawTracks, i, fileOffsetBytes, baseDir, cueFile);

            resolved.add(new CueTrack(track.trackNumber(), runningLba, sectorCount, track.mode(),
                    track.fileName(), fileOffsetBytes));
            runningLba += sectorCount;
        }

        return resolved;
    }

    private static long resolveSectorCount(List<RawTrack> rawTracks, int index, long fileOffsetBytes,
                                            Path baseDir, Path cueFile) throws IOException {
        RawTrack track = rawTracks.get(index);
        RawTrack next = index + 1 < rawTracks.size() ? rawTracks.get(index + 1) : null;

        if (next != null && next.fileName().equals(track.fileName())) {
            long sectorCount = next.indexFrameOffset() - track.indexFrameOffset();
            if (sectorCount <= 0) {
                throw new IOException("CUE file " + cueFile + " has track " + track.trackNumber() +
                        " with a non-increasing INDEX 01 relative to track " + next.trackNumber());
            }
            return sectorCount;
        }

        // Last track referencing this file: its length is whatever's left in the file.
        Path trackFile = baseDir.resolve(track.fileName());
        long fileSize;
        try {
            fileSize = Files.size(trackFile);
        } catch (IOException e) {
            throw new IOException("CUE track " + track.trackNumber() +
                    " references a file that does not exist: " + trackFile, e);
        }

        long remainingBytes = fileSize - fileOffsetBytes;
        if (remainingBytes <= 0) {
            throw new IOException("CUE file " + cueFile + " has track " + track.trackNumber() +
                    "'s INDEX 01 starting at or past the end of " + trackFile);
        }
        int sectorSize = track.mode().sectorSize();
        if (remainingBytes % sectorSize != 0) {
            throw new IOException("CUE file " + cueFile + " has track " + track.trackNumber() +
                    ": remaining bytes in " + trackFile + " (" + remainingBytes +
                    ") is not a whole number of " + sectorSize + "-byte sectors");
        }
        return remainingBytes / sectorSize;
    }

    // ---- Reading -------------------------------------------------------

    /** Returns all tracks, sorted by ascending start LBA. */
    public List<CueTrack> tracks() {
        return tracksByLba;
    }

    /**
     * Finds the track that contains the given LBA (i.e. the last track whose
     * start LBA is less than or equal to it).
     *
     * @throws IllegalArgumentException if the LBA is before the first track
     */
    public CueTrack trackContainingLba(long lba) {
        CueTrack found = null;
        for (CueTrack track : tracksByLba) {
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
     * @throws IOException if the track's data file is missing or can't be read
     */
    public void readSector(long lba, byte[] dest) throws IOException {
        CueTrack track = trackContainingLba(lba);

        if (dest.length < track.sectorSize()) {
            throw new IllegalArgumentException("Destination buffer (" + dest.length +
                    " bytes) is smaller than track " + track.trackNumber() +
                    "'s sector size (" + track.sectorSize() + " bytes)");
        }

        RandomAccessFile file = openTrackFile(track);
        long sectorIndexInTrack = lba - track.startLba();
        long filePosition = track.fileOffset() + sectorIndexInTrack * track.sectorSize();
        file.seek(filePosition);
        file.readFully(dest, 0, track.sectorSize());
    }

    private RandomAccessFile openTrackFile(CueTrack track) throws IOException {
        RandomAccessFile existing = openFilesByName.get(track.fileName());
        if (existing != null) {
            return existing;
        }
        Path trackFile = baseDir.resolve(track.fileName());
        try {
            RandomAccessFile opened = new RandomAccessFile(trackFile.toFile(), "r");
            openFilesByName.put(track.fileName(), opened);
            return opened;
        } catch (FileNotFoundException e) {
            throw new IOException("CUE track " + track.trackNumber() +
                    " references a file that does not exist: " + trackFile, e);
        }
    }

    @Override
    public void close() throws IOException {
        IOException firstError = null;
        for (RandomAccessFile file : openFilesByName.values()) {
            try {
                file.close();
            } catch (IOException e) {
                if (firstError == null) {
                    firstError = e;
                }
            }
        }
        openFilesByName.clear();
        if (firstError != null) {
            throw firstError;
        }
    }
}
