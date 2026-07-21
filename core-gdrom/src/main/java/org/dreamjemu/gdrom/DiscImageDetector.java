package org.dreamjemu.gdrom;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Detects the format of a Dreamcast disc image file, independent of any CPU/GPU work.
 * Flagged in /docs/STATUS.md as a good, self-contained early task: it's easy to validate
 * (feed it real files of each format, assert the right enum comes back) without needing
 * the rest of the emulation core.
 *
 * Detection combines two signals, in order:
 *   1. File extension (fast, usually sufficient).
 *   2. Structural verification of the file's actual content (magic bytes /
 *      expected text structure), so a file with a wrong or missing extension
 *      can still often be identified, and so a file with a matching
 *      extension but garbage content is not misreported as a known format.
 *
 * This only inspects the container/structure of the user-provided file; it
 * never requires or reads any original console/BIOS file — see README.md.
 */
public final class DiscImageDetector {

    /** CHD (MAME "Compressed Hunks of Data") files start with this 8-byte ASCII magic. */
    private static final byte[] CHD_MAGIC = "MComprHD".getBytes(StandardCharsets.US_ASCII);

    /**
     * DiscJuggler CDI images end with a 4-byte little-endian version marker.
     * Known values (v2/v3/v3.5) are 0x00000004, 0x80000004, and 0x80000005.
     * This is a widely documented structural fact about the CDI container
     * format (used across the open-source disc-imaging/emulation community),
     * not anything extracted from copyrighted Sega material.
     */
    private static final long[] CDI_TRAILER_MAGICS = {0x00000004L, 0x80000004L, 0x80000005L};

    private DiscImageDetector() {
    }

    /**
     * Detects the {@link DiscImageFormat} of the given file.
     *
     * @param path path to a candidate disc image file
     * @return the detected format, or {@link DiscImageFormat#UNKNOWN} if it can't be determined
     */
    public static DiscImageFormat detect(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return DiscImageFormat.UNKNOWN;
        }

        String extension = extensionOf(path);

        // Try the format implied by the extension first — cheap, and correct
        // in the overwhelming majority of real-world cases.
        DiscImageFormat byExtension = switch (extension) {
            case "gdi" -> looksLikeGdi(path) ? DiscImageFormat.GDI : DiscImageFormat.UNKNOWN;
            case "cdi" -> looksLikeCdi(path) ? DiscImageFormat.CDI : DiscImageFormat.UNKNOWN;
            case "chd" -> looksLikeChd(path) ? DiscImageFormat.CHD : DiscImageFormat.UNKNOWN;
            case "cue" -> looksLikeCue(path) ? DiscImageFormat.CUE_BIN : DiscImageFormat.UNKNOWN;
            default -> DiscImageFormat.UNKNOWN;
        };

        if (byExtension != DiscImageFormat.UNKNOWN) {
            return byExtension;
        }

        // Extension was missing, wrong, or didn't structurally check out —
        // fall back to probing content directly, in cheapest-check-first order.
        if (looksLikeChd(path)) {
            return DiscImageFormat.CHD;
        }
        if (looksLikeGdi(path)) {
            return DiscImageFormat.GDI;
        }
        if (looksLikeCue(path)) {
            return DiscImageFormat.CUE_BIN;
        }
        if (looksLikeCdi(path)) {
            return DiscImageFormat.CDI;
        }

        return DiscImageFormat.UNKNOWN;
    }

    private static String extensionOf(Path path) {
        String fileName = path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * GDI is a small plain-text index: the first non-empty line is the track
     * count (a positive integer), followed by one line per track. We check
     * just the first line's shape, which is enough to distinguish it from
     * every other supported format without needing a full parser here.
     */
    static boolean looksLikeGdi(Path path) {
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.US_ASCII);
            for (String line : lines) {
                String trimmed = line.strip();
                if (trimmed.isEmpty()) {
                    continue;
                }
                int trackCount = Integer.parseInt(trimmed);
                return trackCount > 0 && trackCount <= 99; // GD-ROM discs have at most a few dozen tracks
            }
            return false;
        } catch (IOException | NumberFormatException e) {
            return false;
        }
    }

    /**
     * A CUE sheet is plain text describing one or more BIN/track files. We
     * look for the two keywords that are structurally required in any valid
     * cue sheet referencing at least one track.
     */
    static boolean looksLikeCue(Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.US_ASCII).toUpperCase(Locale.ROOT);
            return content.contains("FILE ") && content.contains("TRACK ");
        } catch (IOException e) {
            return false;
        }
    }

    /** CHD files start with the fixed 8-byte ASCII magic "MComprHD". */
    static boolean looksLikeChd(Path path) {
        byte[] header = readBytes(path, 0, CHD_MAGIC.length);
        return header != null && java.util.Arrays.equals(header, CHD_MAGIC);
    }

    /** CDI files end with a small trailer whose last 4 bytes are a known version marker. */
    static boolean looksLikeCdi(Path path) {
        try {
            long fileSize = Files.size(path);
            if (fileSize < 4) {
                return false;
            }
            byte[] trailer = readBytes(path, fileSize - 4, 4);
            if (trailer == null) {
                return false;
            }
            long marker = ((long) (trailer[3] & 0xFF) << 24)
                    | ((trailer[2] & 0xFF) << 16)
                    | ((trailer[1] & 0xFF) << 8)
                    | (trailer[0] & 0xFF);
            for (long known : CDI_TRAILER_MAGICS) {
                if (marker == known) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private static byte[] readBytes(Path path, long offset, int length) {
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
            if (file.length() < offset + length) {
                return null;
            }
            file.seek(offset);
            byte[] buffer = new byte[length];
            file.readFully(buffer);
            return buffer;
        } catch (IOException e) {
            return null;
        }
    }
}
