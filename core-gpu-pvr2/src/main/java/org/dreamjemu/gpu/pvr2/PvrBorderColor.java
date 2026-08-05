package org.dreamjemu.gpu.pvr2;

/**
 * The border color shown outside the active display area (e.g. VGA
 * overscan borders).
 *
 * <p>Source: "Guide to the PowerVR-chip of the Dreamcast" by Lars Olsson,
 * v0.51 (hitmen.c02.at/files/docs/dc/powervr-reg.txt), register
 * {@code a05f8040 (border_col)}:
 *
 * <pre>
 * bits 31-24 : n/a
 * bits 23-0  : col (border color, in RGB888 format)
 * </pre>
 *
 * <p>The source describes {@code col} as a single 24-bit "RGB888-format"
 * value without breaking out which byte is which channel — unlike, say,
 * {@code fb_render_cfg}, which the same source spells out field-by-field.
 * This class assumes the standard RGB888 byte order (red in bits 23-16,
 * green in bits 15-8, blue in bits 7-0), the same convention used
 * everywhere else RGB888 appears (web colors, most framebuffer formats,
 * and this same document's own {@code fog_table_col}/{@code
 * fog_vertex_col} registers). Reasonable, but not something the source
 * spells out explicitly for this specific register — flagged here rather
 * than left silently assumed.
 */
public record PvrBorderColor(int red, int green, int blue) {

    /** P2 (uncached) address of this register. */
    public static final int REGISTER_ADDRESS = 0xA05F8040;

    public PvrBorderColor {
        requireByte("red", red);
        requireByte("green", green);
        requireByte("blue", blue);
    }

    private static void requireByte(String name, int value) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(name + " must be 0-255, got " + value);
        }
    }

    public int encode() {
        return (red << 16) | (green << 8) | blue;
    }

    public static PvrBorderColor decode(int value) {
        int red = (value >>> 16) & 0xFF;
        int green = (value >>> 8) & 0xFF;
        int blue = value & 0xFF;
        return new PvrBorderColor(red, green, blue);
    }
}
