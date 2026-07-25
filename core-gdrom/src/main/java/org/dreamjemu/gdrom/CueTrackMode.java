package org.dreamjemu.gdrom;

import java.util.Locale;

/**
 * Track mode as declared on a CUE sheet's {@code TRACK NN <mode>} line. Each
 * mode implies a fixed sector size, which is what {@link CueBinImage} needs
 * to turn a byte offset in a .bin file into sector-sized reads.
 *
 * This covers the modes actually seen in Dreamcast GD-ROM / redump-style
 * CUE/BIN dumps (data tracks are effectively always {@code MODE1/2352} or
 * {@code MODE2/2352} in practice), plus the plain {@code AUDIO} mode shared
 * with ordinary Red Book audio CDs.
 */
public enum CueTrackMode {
    AUDIO("AUDIO", 2352, true),
    MODE1_2048("MODE1/2048", 2048, false),
    MODE1_2352("MODE1/2352", 2352, false),
    MODE2_2048("MODE2/2048", 2048, false),
    MODE2_2336("MODE2/2336", 2336, false),
    MODE2_2352("MODE2/2352", 2352, false);

    private final String token;
    private final int sectorSize;
    private final boolean audio;

    CueTrackMode(String token, int sectorSize, boolean audio) {
        this.token = token;
        this.sectorSize = sectorSize;
        this.audio = audio;
    }

    public int sectorSize() {
        return sectorSize;
    }

    public boolean isAudio() {
        return audio;
    }

    /**
     * Parses the mode token as it appears on a CUE sheet's TRACK line (e.g.
     * {@code "MODE1/2352"} or {@code "AUDIO"}), case-insensitively.
     *
     * @throws IllegalArgumentException if the token is not a recognized mode
     */
    public static CueTrackMode fromToken(String token) {
        String normalized = token.strip().toUpperCase(Locale.ROOT);
        for (CueTrackMode mode : values()) {
            if (mode.token.equals(normalized)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown CUE track mode: \"" + token + "\"");
    }
}
