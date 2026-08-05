package org.dreamjemu.gpu.pvr2;

/**
 * The render output configuration register: alpha threshold/insertion
 * values, dithering, and the framebuffer pixel format the renderer
 * writes.
 *
 * <p>Source: "Guide to the PowerVR-chip of the Dreamcast" by Lars Olsson,
 * v0.51 (hitmen.c02.at/files/docs/dc/powervr-reg.txt), register
 * {@code a05f8048 (fb_render_cfg)}:
 *
 * <pre>
 * bits 31-24 : n/a
 * bits 23-16 : threshold  (threshold for alpha transparency)
 * bits 15-8  : alpha      (inserted into the alpha channel for formats that support it)
 * bits 7-4   : n/a
 * bit  3     : dither     (0 = disabled, 1 = enabled, only for 2-byte/pixel formats)
 * bits 2-0   : render mode (see {@link PvrRenderPixelFormat})
 * </pre>
 */
public record PvrRenderConfig(int threshold, int alpha, boolean ditherEnabled, PvrRenderPixelFormat pixelFormat) {

    /** P2 (uncached) address of this register. */
    public static final int REGISTER_ADDRESS = 0xA05F8048;

    private static final int THRESHOLD_SHIFT = 16;
    private static final int ALPHA_SHIFT = 8;
    private static final int DITHER_BIT = 3;
    private static final int FORMAT_MASK = 0b111;

    public PvrRenderConfig {
        if (threshold < 0 || threshold > 0xFF) {
            throw new IllegalArgumentException("threshold must be 0-255, got " + threshold);
        }
        if (alpha < 0 || alpha > 0xFF) {
            throw new IllegalArgumentException("alpha must be 0-255, got " + alpha);
        }
    }

    public int encode() {
        int value = (threshold & 0xFF) << THRESHOLD_SHIFT;
        value |= (alpha & 0xFF) << ALPHA_SHIFT;
        if (ditherEnabled) {
            value |= 1 << DITHER_BIT;
        }
        value |= pixelFormat.fieldValue() & FORMAT_MASK;
        return value;
    }

    public static PvrRenderConfig decode(int value) {
        int threshold = (value >>> THRESHOLD_SHIFT) & 0xFF;
        int alpha = (value >>> ALPHA_SHIFT) & 0xFF;
        boolean ditherEnabled = ((value >>> DITHER_BIT) & 1) != 0;
        PvrRenderPixelFormat pixelFormat = PvrRenderPixelFormat.fromFieldValue(value & FORMAT_MASK);
        return new PvrRenderConfig(threshold, alpha, ditherEnabled, pixelFormat);
    }
}
