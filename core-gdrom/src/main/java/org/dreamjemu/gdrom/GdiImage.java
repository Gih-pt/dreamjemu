package org.dreamjemu.gdrom;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses a GDI disc image and provides sector-level reads from its track
 * files. This is the first concrete "reading" capability in core-gdrom,
 * built on top of the format detection in {@link DiscImageDetector} — see
 * /docs/ROADMAP.md.
 *
 * A GDI file is a small plain-text index: the first non-empty line is the
 * track count, followed by one line per track:
 * {@code trackNumber startLba typeCode sectorSize fileName offset}.
 * Track file names are resolved relative to the .gdi file's own directory.
 *
 * This class only reads whatever files the user already has locally; it
 * never requires or reads any original console/BIOS file.
 */
public final class GdiImage implements AutoCloseable {

    private final Path baseDir;
    private final List<GdiTrack> tracksByLba; // sorted ascending by startLba
    private final Map<Integer, RandomAccessFile> openTrackFiles = new HashMap<>();

    private GdiImage(Path baseDir, List<GdiTrack> tracksByLba) {
        this.baseDir = baseDir;
        this.tracksByLba = tracksByLba;
    }

    /**
     * Parses the given .gdi file. Does not open any track data files yet —
     * those are opened lazily on first read, in {@link #readSector}.
     *
     * @throws IOException if the file can't be read, or its structure is invalid
     *                      (missing/mismatched track count, malformed track line)
     */
    public static GdiImage load(Path gdiFile) throws IOException {
        List<String> lines = Files.readAllLines(gdiFile);

        int declaredTrackCount = -1;
        List<GdiTrack> tracks = new ArrayList<>();

        for (String rawLine : lines) {
            String line = rawLine.strip();
            if (line.isEmpty()) {
                continue;
            }
            if (declaredTrackCount < 0) {
                declaredTrackCount = parseTrackCount(line, gdiFile);
                continue;
            }
            tracks.add(parseTrackLine(line, gdiFile));
        }

        if (declaredTrackCount < 0) {
            throw new IOException("GDI file has no track count line: " + gdiFile);
        }
        if (tracks.size() != declaredTrackCount) {
            throw new IOException("GDI file " + gdiFile + " declares " + declaredTrackCount +
                    " track(s) but " + tracks.size() + " track line(s) were found");
        }

        List<GdiTrack> sorted = new ArrayList<>(tracks);
        sorted.sort(Comparator.comparingLong(GdiTrack::startLba));

        return new GdiImage(gdiFile.toAbsolutePath().getParent(), Collections.unmodifiableList(sorted));
    }

    private static int parseTrackCount(String line, Path gdiFile) throws IOException {
        try {
            int count = Integer.parseInt(line);
            if (count <= 0) {
                throw new IOException("GDI file " + gdiFile + " declares a non-positive track count: " + count);
            }
            return count;
        } catch (NumberFormatException e) {
            throw new IOException("GDI file " + gdiFile + " has an invalid track count line: \"" + line + "\"", e);
        }
    }

    private static GdiTrack parseTrackLine(String line, Path gdiFile) throws IOException {
        String[] parts = line.split("\\s+", 6);
        if (parts.length < 6) {
            throw new IOException("Malformed GDI track line in " + gdiFile + ": \"" + line + "\"");
        }
        try {
            int trackNumber = Integer.parseInt(parts[0]);
            long startLba = Long.parseLong(parts[1]);
            GdiTrackType type = GdiTrackType.fromCode(Integer.parseInt(parts[2]));
            int sectorSize = Integer.parseInt(parts[3]);
            String fileName = stripQuotes(parts[4]);
            long offset = Long.parseLong(parts[5]);
            return new GdiTrack(trackNumber, startLba, type, sectorSize, fileName, offset);
        } catch (IllegalArgumentException e) {
            throw new IOException("Malformed GDI track line in " + gdiFile + ": \"" + line + "\"", e);
        }
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /** Returns all tracks, sorted by ascending start LBA. */
    public List<GdiTrack> tracks() {
        return tracksByLba;
    }

    /**
     * Finds the track that contains the given LBA (i.e. the last track whose
     * start LBA is less than or equal to it).
     *
     * @throws IllegalArgumentException if the LBA is before the first track
     */
    public GdiTrack trackContainingLba(long lba) {
        GdiTrack found = null;
        for (GdiTrack track : tracksByLba) {
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
        GdiTrack track = trackContainingLba(lba);

        if (dest.length < track.sectorSize()) {
            throw new IllegalArgumentException("Destination buffer (" + dest.length +
                    " bytes) is smaller than track " + track.trackNumber() +
                    "'s sector size (" + track.sectorSize() + " bytes)");
        }

        RandomAccessFile file = openTrackFile(track);
        long sectorIndexInTrack = lba - track.startLba();
        long filePosition = track.offset() + sectorIndexInTrack * track.sectorSize();
        file.seek(filePosition);
        file.readFully(dest, 0, track.sectorSize());
    }

    private RandomAccessFile openTrackFile(GdiTrack track) throws IOException {
        RandomAccessFile existing = openTrackFiles.get(track.trackNumber());
        if (existing != null) {
            return existing;
        }
        Path trackFile = baseDir.resolve(track.fileName());
        try {
            RandomAccessFile opened = new RandomAccessFile(trackFile.toFile(), "r");
            openTrackFiles.put(track.trackNumber(), opened);
            return opened;
        } catch (FileNotFoundException e) {
            throw new IOException("GDI track " + track.trackNumber() +
                    " references a file that does not exist: " + trackFile, e);
        }
    }

    @Override
    public void close() throws IOException {
        IOException firstError = null;
        for (RandomAccessFile file : openTrackFiles.values()) {
            try {
                file.close();
            } catch (IOException e) {
                if (firstError == null) {
                    firstError = e;
                }
            }
        }
        openTrackFiles.clear();
        if (firstError != null) {
            throw firstError;
        }
    }
}
