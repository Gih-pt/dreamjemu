package org.dreamjemu.gpu.pvr2;

/**
 * The pixel format the renderer writes to the framebuffer, selected by
 * {@code fb_render_cfg}'s 3-bit {@code render mode} field. Distinct from
 * {@link PvrDisplayPixelFormat}, a different (2-bit) field on a different
 * register ({@code fb_display_cfg}) for the display *output* rather than
 * the render target.
 *
 * <p>Source: "Guide to the PowerVR-chip of the Dreamcast" by Lars Olsson,
 * v0.51 (hitmen.c02.at/files/docs/dc/powervr-reg.txt), register
 * {@code a05f8048 (fb_render_cfg)}:
 *
 * <pre>
 * 0: RGB0555  (2 bytes/pixel, alpha is inserted into bit15)
 * 1: RGB565   (2 bytes/pixel)
 * 2: ARGB4444 (2 bytes/pixel)
 * 3: ARGB1555 (2 bytes/pixel, alpha is determined by threshold)
 * 4: RGB888   (3 bytes/pixel)
 * 5: RGB0888  (4 bytes/pixel, alpha is inserted into bit24-31)
 * 6: ARGB8888 (4 bytes/pixel)
 * 7: ARGB4444 (2 bytes/pixel, same as 2?)
 * </pre>
 *
 * <p><b>Note the source's own trailing "same as 2?"</b> on value 7 — this
 * is the source questioning its own entry, not a typo introduced here.
 * {@code ARGB4444_ALTERNATE} preserves that value as its own distinct
 * constant rather than collapsing it into {@code ARGB4444}, since two
 * field values that decode to the same enum constant would make
 * {@code encode()}/{@code decode()} stop round-tripping for one of them —
 * silently discarding real hardware behavior (whatever the difference
 * between 2 and 7 actually is, if any) to resolve an uncertainty the
 * source itself left open.
 *
 * <p>This is the exact register whose bit layout an earlier
 * {@code core-gpu-pvr2} contribution flagged as unimplemented after
 * {@code mc.pp.se/dc/pvr.html}'s table for it extracted with merged
 * columns that couldn't be read with confidence (see that entry's
 * CHANGELOG note) — implemented now using this cleaner source instead.
 */
public enum PvrRenderPixelFormat {
    RGB0555(0),
    RGB565(1),
    ARGB4444(2),
    ARGB1555(3),
    RGB888(4),
    RGB0888(5),
    ARGB8888(6),
    ARGB4444_ALTERNATE(7);

    private final int fieldValue;

    PvrRenderPixelFormat(int fieldValue) {
        this.fieldValue = fieldValue;
    }

    public int fieldValue() {
        return fieldValue;
    }

    public static PvrRenderPixelFormat fromFieldValue(int fieldValue) {
        for (PvrRenderPixelFormat format : values()) {
            if (format.fieldValue == fieldValue) {
                return format;
            }
        }
        throw new IllegalArgumentException("Render pixel format field must be 0-7, got " + fieldValue);
    }
}
