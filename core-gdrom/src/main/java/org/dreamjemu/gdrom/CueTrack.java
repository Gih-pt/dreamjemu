package org.dreamjemu.gdrom;

/**
 * A single track resolved from a CUE sheet, with everything
 * {@link CueBinImage#readSector} needs already computed: its global LBA
 * range (consistent with the whole-disc LBA numbering used elsewhere in
 * core-gdrom, e.g. {@link GdiTrack}) and its byte offset into its own
 * .bin file.
 *
 * @param trackNumber  1-based track number, as declared in the CUE sheet
 * @param startLba     global LBA where this track begins (track 1 starts at LBA 0)
 * @param sectorCount  number of sectors in this track
 * @param mode         track mode (implies sector size and audio/data)
 * @param fileName     the .bin file this track's data lives in, as declared in the
 *                     CUE sheet (relative to the CUE file's own directory)
 * @param fileOffset   byte offset into {@code fileName} where this track's first sector starts
 */
public record CueTrack(
        int trackNumber,
        long startLba,
        long sectorCount,
        CueTrackMode mode,
        String fileName,
        long fileOffset
) {
    public int sectorSize() {
        return mode.sectorSize();
    }
}
