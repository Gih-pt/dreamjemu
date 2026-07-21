package org.dreamjemu.gdrom;

/**
 * Dreamcast disc image formats the emulator should be able to auto-detect and read.
 * See /docs/ROADMAP.md Phase 1: "GD-ROM: disc image format detection and reading".
 *
 * No format here requires an original BIOS or console file — detection and reading
 * operate purely on the user-provided disc image file itself.
 */
public enum DiscImageFormat {
    GDI,
    CDI,
    CHD,
    CUE_BIN,
    UNKNOWN;

    // Detection lives in DiscImageDetector, not here, so this enum stays a
    // plain value type with no I/O dependencies.
}
