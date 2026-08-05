package org.dreamjemu.gpu.pvr2;

/**
 * The color format of the 1024-entry texture palette table
 * ({@code palette_table}, {@code a05f9000-a05f9fff}), selected by the
 * {@code palette_cfg} register's 2-bit {@code mode} field.
 *
 * <p>Source: "Guide to the PowerVR-chip of the Dreamcast" by Lars Olsson,
 * v0.51 (hitmen.c02.at/files/docs/dc/powervr-reg.txt), register
 * {@code a05f8108 (palette_cfg)}:
 *
 * <pre>
 * 0: ARGB1555
 * 1: RGB565
 * 2: ARGB4444
 * 3: ARGB8888
 * </pre>
 */
public enum PvrPaletteMode {
    ARGB1555(0),
    RGB565(1),
    ARGB4444(2),
    ARGB8888(3);

    private final int fieldValue;

    PvrPaletteMode(int fieldValue) {
        this.fieldValue = fieldValue;
    }

    public int fieldValue() {
        return fieldValue;
    }

    public static PvrPaletteMode fromFieldValue(int fieldValue) {
        for (PvrPaletteMode mode : values()) {
            if (mode.fieldValue == fieldValue) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Palette mode field must be 0-3, got " + fieldValue);
    }
}
