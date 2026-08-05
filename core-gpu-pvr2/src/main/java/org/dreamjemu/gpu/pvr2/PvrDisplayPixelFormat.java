package org.dreamjemu.gpu.pvr2;

/**
 * The pixel format used for the display output, selected by
 * {@code fb_display_cfg}'s 2-bit {@code pixelmode} field. Distinct from
 * {@link PvrRenderPixelFormat}, which is a different (3-bit, differently
 * numbered) field on a different register ({@code fb_render_cfg}) for the
 * *render* target rather than the display output.
 *
 * <p>Source: "Guide to the PowerVR-chip of the Dreamcast" by Lars Olsson,
 * v0.51 (hitmen.c02.at/files/docs/dc/powervr-reg.txt), register
 * {@code a05f8044 (fb_display_cfg)}, "(alphamode is not set)":
 *
 * <pre>
 * 0: RGB0555 (2 bytes/pixel)
 * 1: RGB565  (2 bytes/pixel)
 * 2: RGB888  (3 bytes/pixel)
 * 3: RGB0888 (4 bytes/pixel)
 * </pre>
 */
public enum PvrDisplayPixelFormat {
    RGB0555(0),
    RGB565(1),
    RGB888(2),
    RGB0888(3);

    private final int fieldValue;

    PvrDisplayPixelFormat(int fieldValue) {
        this.fieldValue = fieldValue;
    }

    public int fieldValue() {
        return fieldValue;
    }

    public static PvrDisplayPixelFormat fromFieldValue(int fieldValue) {
        for (PvrDisplayPixelFormat format : values()) {
            if (format.fieldValue == fieldValue) {
                return format;
            }
        }
        throw new IllegalArgumentException("Display pixel format field must be 0-3, got " + fieldValue);
    }
}
