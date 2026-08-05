package org.dreamjemu.gpu.pvr2;

/**
 * The palette configuration register: selects the color format used by
 * the 1024-entry texture palette table.
 *
 * <p>Source: "Guide to the PowerVR-chip of the Dreamcast" by Lars Olsson,
 * v0.51 (hitmen.c02.at/files/docs/dc/powervr-reg.txt), register
 * {@code a05f8108 (palette_cfg)}:
 *
 * <pre>
 * bits 31-2 : n/a
 * bits 1-0  : mode (see {@link PvrPaletteMode})
 * </pre>
 */
public record PvrPaletteConfig(PvrPaletteMode mode) {

    /** P2 (uncached) address of this register. */
    public static final int REGISTER_ADDRESS = 0xA05F8108;

    private static final int MODE_MASK = 0b11;

    public int encode() {
        return mode.fieldValue() & MODE_MASK;
    }

    public static PvrPaletteConfig decode(int value) {
        return new PvrPaletteConfig(PvrPaletteMode.fromFieldValue(value & MODE_MASK));
    }
}
