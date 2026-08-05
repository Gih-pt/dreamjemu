package org.dreamjemu.gpu.pvr2;

/**
 * The fog density coefficient for TSP fogging, expressed as a
 * mantissa/exponent pair (i.e. {@code mantissa * 2^exponent}, though the
 * source doesn't spell out the exact combination formula beyond calling
 * {@code exponent} a "power of 2" — that reconstruction is left to
 * whatever future component actually applies fogging, not assumed here).
 *
 * <p>Source: "Guide to the PowerVR-chip of the Dreamcast" by Lars Olsson,
 * v0.51 (hitmen.c02.at/files/docs/dc/powervr-reg.txt), register
 * {@code a05f80b8 (fog_density)}:
 *
 * <pre>
 * bits 31-16 : n/a
 * bits 15-8  : mantissa
 * bits 7-0   : exponent (power of 2)
 * </pre>
 */
public record PvrFogDensity(int mantissa, int exponent) {

    /** P2 (uncached) address of this register. */
    public static final int REGISTER_ADDRESS = 0xA05F80B8;

    private static final int MANTISSA_SHIFT = 8;

    public PvrFogDensity {
        if (mantissa < 0 || mantissa > 0xFF) {
            throw new IllegalArgumentException("mantissa must be 0-255, got " + mantissa);
        }
        if (exponent < 0 || exponent > 0xFF) {
            throw new IllegalArgumentException("exponent must be 0-255, got " + exponent);
        }
    }

    public int encode() {
        return ((mantissa & 0xFF) << MANTISSA_SHIFT) | (exponent & 0xFF);
    }

    public static PvrFogDensity decode(int value) {
        int mantissa = (value >>> MANTISSA_SHIFT) & 0xFF;
        int exponent = value & 0xFF;
        return new PvrFogDensity(mantissa, exponent);
    }
}
