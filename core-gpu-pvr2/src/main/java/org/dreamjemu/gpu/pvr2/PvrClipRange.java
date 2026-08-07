package org.dreamjemu.gpu.pvr2;

/**
 * A pixel clipping range — shared shape used by both the horizontal
 * ({@code fb_clip_x}) and vertical ({@code fb_clip_y}) clipping
 * registers, which have identical bit layouts.
 *
 * <p>Source: "Guide to the PowerVR-chip of the Dreamcast" by Lars Olsson,
 * v0.51 (hitmen.c02.at/files/docs/dc/powervr-reg.txt):
 *
 * <pre>
 * a05f8068 (fb_clip_x) / a05f806c (fb_clip_y)
 * +-------------------------------+
 * | 31-27 | 26-16 | 15-11 | 10-0  |
 * |  n/a  | max   |  n/a  | min   |
 * +-------------------------------+
 * "Specifies the horizontal/vertical pixel clipping area - 1."
 * </pre>
 *
 * <p>The source's "- 1" is a single sentence describing the register as
 * a whole, without saying whether it applies to {@code max}, to
 * {@code min}, to both, or to the size of the area they bound together —
 * unlike e.g. {@code tilebuf_size}, which spells out "minus 1" against
 * each field individually. Rather than guess which reading is correct,
 * {@link #min()}/{@link #max()} here store the two 11-bit fields exactly
 * as the hardware carries them, with no assumed transformation — the
 * "- 1" adjustment (whatever it turns out to mean) is left to whatever
 * higher-level clipping logic is built on top of this class later.
 */
public record PvrClipRange(int min, int max) {

    /** P2 (uncached) address of the horizontal clip register ({@code fb_clip_x}). */
    public static final int HORIZONTAL_REGISTER_ADDRESS = 0xA05F8068;

    /** P2 (uncached) address of the vertical clip register ({@code fb_clip_y}). */
    public static final int VERTICAL_REGISTER_ADDRESS = 0xA05F806C;

    private static final int FIELD_MASK = 0x7FF;
    private static final int MAX_SHIFT = 16;

    public PvrClipRange {
        if (min < 0 || min > FIELD_MASK) {
            throw new IllegalArgumentException("min must be 0-" + FIELD_MASK + ", got " + min);
        }
        if (max < 0 || max > FIELD_MASK) {
            throw new IllegalArgumentException("max must be 0-" + FIELD_MASK + ", got " + max);
        }
    }

    public int encode() {
        return ((max & FIELD_MASK) << MAX_SHIFT) | (min & FIELD_MASK);
    }

    public static PvrClipRange decode(int value) {
        int max = (value >>> MAX_SHIFT) & FIELD_MASK;
        int min = value & FIELD_MASK;
        return new PvrClipRange(min, max);
    }
}
