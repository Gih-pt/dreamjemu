package org.dreamjemu.gdrom;

/**
 * A single track entry parsed from a GDI file's track list.
 *
 * @param trackNumber 1-based track number, as declared in the GDI file
 * @param startLba    logical block address where this track begins
 * @param type        audio or data
 * @param sectorSize  bytes per sector for this track (commonly 2352)
 * @param fileName    track's data file name, as declared in the GDI (relative to the GDI file's directory)
 * @param offset      byte offset into the track file where sector data starts (commonly 0)
 */
public record GdiTrack(
        int trackNumber,
        long startLba,
        GdiTrackType type,
        int sectorSize,
        String fileName,
        long offset
) {
}
