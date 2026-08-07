package org.dreamjemu.gpu.pvr2;

/**
 * "Cheap shadow" configuration: a simplified shadowing effect controlled
 * by modifier volumes, without the cost of full shadow volume rendering.
 *
 * <p>Source: "Guide to the PowerVR-chip of the Dreamcast" by Lars Olsson,
 * v0.51 (hitmen.c02.at/files/docs/dc/powervr-reg.txt), register
 * {@code a05f8074 (shadow)}:
 *
 * <pre>
 * bits 31-9 : n/a
 * bit  8    : enable (0 = cheap shadow disabled, 1 = enabled)
 * bits 7-0  : intensity (how much cheap shadow affects a polygon,
 *             depending on how far away the modifier volume is located)
 * </pre>
 */
public record PvrShadowConfig(boolean enabled, int intensity) {

    /** P2 (uncached) address of this register. */
    public static final int REGISTER_ADDRESS = 0xA05F8074;

    private static final int ENABLE_BIT = 8;

    public PvrShadowConfig {
        if (intensity < 0 || intensity > 0xFF) {
            throw new IllegalArgumentException("intensity must be 0-255, got " + intensity);
        }
    }

    public int encode() {
        int value = intensity & 0xFF;
        if (enabled) {
            value |= 1 << ENABLE_BIT;
        }
        return value;
    }

    public static PvrShadowConfig decode(int value) {
        boolean enabled = ((value >>> ENABLE_BIT) & 1) != 0;
        int intensity = value & 0xFF;
        return new PvrShadowConfig(enabled, intensity);
    }
}
