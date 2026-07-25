package org.dreamjemu.gdrom;

/**
 * A single track resolved from a DiscJuggler CDI image. Unlike CUE, a CDI
 * header declares each track's global LBA and sector count directly, so
 * (unlike {@link CueTrack}) nothing here needs to be derived — this record
 * just holds what {@link CdiImage#readSector} needs, already computed by
 * the parser: the byte offset in the (single, monolithic) .cdi file where
 * this track's sector 0 begins, which already accounts for skipping the
 * track's pregap.
 *
 * @param sessionNumber 1-based session number (CDI supports multiple
 *                      sessions per disc; Dreamcast images have at most 2)
 * @param trackNumber   1-based track number, counted across all sessions
 * @param startLba      global LBA where this track begins, as declared in the CDI header
 * @param sectorCount   number of sectors in this track (excluding its pregap)
 * @param mode          track mode (implies which sector sizes are valid)
 * @param sectorSize    this track's actual sector size in bytes
 * @param fileOffset    byte offset into the .cdi file where this track's first sector starts
 */
public record CdiTrack(
        int sessionNumber,
        int trackNumber,
        long startLba,
        long sectorCount,
        CdiTrackMode mode,
        int sectorSize,
        long fileOffset
) {
}
