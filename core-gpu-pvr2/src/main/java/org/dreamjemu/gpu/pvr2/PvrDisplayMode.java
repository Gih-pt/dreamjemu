package org.dreamjemu.gpu.pvr2;

/**
 * The main display configuration register: pixel clock rate, pixel
 * format, and whether display output is enabled at all.
 *
 * <p>Source: "Guide to the PowerVR-chip of the Dreamcast" by Lars Olsson,
 * v0.51 (hitmen.c02.at/files/docs/dc/powervr-reg.txt), register
 * {@code a05f8044 (fb_display_cfg)}:
 *
 * <pre>
 * bit  23    : clock       (0 = normal, 1 = pixel clock doubled, for VGA mode)
 * bit  22    : stripen     (0 = no strip buffer, 1 = enable strip buffer)
 * bits 21-16 : striplen    (documented only as "under investigation" - not modeled)
 * bits 15-8  : threshold   (comparison value, only meaningful in ARGB8888 mode)
 * bit  7     : n/a
 * bits 6-4   : extend      (inserted into the low bits to extend RGB555/RGB565 to RGB888)
 * bits 3-2   : pixelmode   (see {@link PvrDisplayPixelFormat})
 * bit  1     : linedouble  (0 = normal, 1 = scanlines sent twice, for 240-line modes)
 * bit  0     : enable      (0 = display disabled, 1 = display enabled)
 * </pre>
 *
 * <p>{@code striplen} is left unmodeled, the same "don't guess at a field
 * the source itself flags as unresolved" call already made for
 * {@code ta_opb_cfg}'s reserved bit 2 and {@code fb_display_cfg}'s own
 * bit 7 — {@code encode()} always writes 0 there, {@code decode()}
 * ignores whatever is read back from it.
 */
public record PvrDisplayMode(boolean pixelClockDoubled, boolean stripBufferEnabled, int threshold, int extend,
                              PvrDisplayPixelFormat pixelFormat, boolean lineDoubled, boolean displayEnabled) {

    /** P2 (uncached) address of this register. */
    public static final int REGISTER_ADDRESS = 0xA05F8044;

    private static final int CLOCK_BIT = 23;
    private static final int STRIPEN_BIT = 22;
    private static final int THRESHOLD_SHIFT = 8;
    private static final int EXTEND_SHIFT = 4;
    private static final int EXTEND_MASK = 0b111;
    private static final int PIXELMODE_SHIFT = 2;
    private static final int PIXELMODE_MASK = 0b11;
    private static final int LINEDOUBLE_BIT = 1;
    private static final int ENABLE_BIT = 0;

    public PvrDisplayMode {
        if (threshold < 0 || threshold > 0xFF) {
            throw new IllegalArgumentException("threshold must be 0-255, got " + threshold);
        }
        if (extend < 0 || extend > EXTEND_MASK) {
            throw new IllegalArgumentException("extend must be 0-7, got " + extend);
        }
    }

    public int encode() {
        int value = 0;
        if (pixelClockDoubled) {
            value |= 1 << CLOCK_BIT;
        }
        if (stripBufferEnabled) {
            value |= 1 << STRIPEN_BIT;
        }
        value |= (threshold & 0xFF) << THRESHOLD_SHIFT;
        value |= (extend & EXTEND_MASK) << EXTEND_SHIFT;
        value |= (pixelFormat.fieldValue() & PIXELMODE_MASK) << PIXELMODE_SHIFT;
        if (lineDoubled) {
            value |= 1 << LINEDOUBLE_BIT;
        }
        if (displayEnabled) {
            value |= 1 << ENABLE_BIT;
        }
        return value;
    }

    public static PvrDisplayMode decode(int value) {
        boolean pixelClockDoubled = ((value >>> CLOCK_BIT) & 1) != 0;
        boolean stripBufferEnabled = ((value >>> STRIPEN_BIT) & 1) != 0;
        int threshold = (value >>> THRESHOLD_SHIFT) & 0xFF;
        int extend = (value >>> EXTEND_SHIFT) & EXTEND_MASK;
        PvrDisplayPixelFormat pixelFormat = PvrDisplayPixelFormat.fromFieldValue((value >>> PIXELMODE_SHIFT) & PIXELMODE_MASK);
        boolean lineDoubled = ((value >>> LINEDOUBLE_BIT) & 1) != 0;
        boolean displayEnabled = ((value >>> ENABLE_BIT) & 1) != 0;
        return new PvrDisplayMode(pixelClockDoubled, stripBufferEnabled, threshold, extend, pixelFormat, lineDoubled, displayEnabled);
    }
}
