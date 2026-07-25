package org.dreamjemu.gdrom;

/**
 * A DiscJuggler CDI track's "mode" field, plus how it combines with the
 * track's separate "sector size code" field to determine the actual
 * sector size in bytes. Unlike CUE's {@code MODE1/2352}-style single token,
 * CDI stores these as two small integers that only some combinations of
 * which are valid — mirrored here from the structural facts documented in
 * the open-source DreamShell project's DiscJuggler CDI reader
 * ({@code include/isofs/cdi.h}, {@code modules/isofs/cdi.c}), which this
 * class's logic (not code) is derived from.
 */
public enum CdiTrackMode {
    /** Red Book CD-DA (audio) track. */
    CDDA(0),
    /** CD-ROM Mode 1 (plain 2048-byte data sectors). */
    DATA(1),
    /** CD-ROM XA Mode 2 (either 2048-byte "cooked" or 2336-byte "semi-raw" sectors). */
    MULTI(2);

    private final int code;

    CdiTrackMode(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    /** Parses the raw "mode" field value read from a CDI track record. */
    public static CdiTrackMode fromCode(long code) {
        for (CdiTrackMode mode : values()) {
            if (mode.code == code) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown CDI track mode code: " + code);
    }

    /**
     * Resolves the actual sector size in bytes for this mode, given the
     * track's separate "sector size code" field (0 = 2048-byte "data", 1 =
     * 2336-byte "semi-raw", 2 = 2352-byte "CDDA"). Not every mode accepts
     * every code — an invalid combination means a corrupt or unsupported
     * CDI image.
     *
     * @throws IllegalArgumentException if the combination isn't valid
     */
    public int sectorSizeFor(long sectorSizeCode) {
        return switch (this) {
            case CDDA -> sectorSizeCode == 2 ? 2352 : invalidCombination(sectorSizeCode);
            case DATA -> sectorSizeCode == 0 ? 2048 : invalidCombination(sectorSizeCode);
            case MULTI -> switch ((int) sectorSizeCode) {
                case 0 -> 2048;
                case 1 -> 2336;
                default -> invalidCombination(sectorSizeCode);
            };
        };
    }

    private int invalidCombination(long sectorSizeCode) {
        throw new IllegalArgumentException(
                "Unsupported sector-size code " + sectorSizeCode + " for CDI track mode " + this);
    }
}
