package org.dreamjemu.gpu.pvr2;

/**
 * The base address of the Object Buffer in VRAM — where the Tile
 * Accelerator stores the vertex data it receives during scene
 * registration, before a render begins.
 *
 * <p>Source: "Guide to the PowerVR-chip of the Dreamcast" by Lars Olsson,
 * v0.51 (hitmen.c02.at/files/docs/dc/powervr-reg.txt), register
 * {@code a05f8020 (ob_addr)}:
 *
 * <pre>
 * bits 31-24 : n/a
 * bits 23-0  : base (bits 0-19 are always zero)
 * </pre>
 *
 * <p>"Bits 0-19 are always zero" means the effective address is
 * 1 MiB-aligned (only bits 23-20 of the field ever vary) — enforced here
 * rather than silently masked away, so a misaligned value fails loudly
 * at construction instead of being truncated without the caller
 * noticing.
 */
public record PvrObjectBufferAddress(int baseAddress) {

    /** P2 (uncached) address of this register. */
    public static final int REGISTER_ADDRESS = 0xA05F8020;

    private static final int FIELD_MASK = 0xFFFFFF;
    private static final int ALIGNMENT_MASK = 0xFFFFF;

    public PvrObjectBufferAddress {
        if ((baseAddress & ~FIELD_MASK) != 0) {
            throw new IllegalArgumentException("baseAddress must fit in 24 bits, got 0x" + Integer.toHexString(baseAddress));
        }
        if ((baseAddress & ALIGNMENT_MASK) != 0) {
            throw new IllegalArgumentException(
                    "baseAddress must be 1 MiB-aligned (bits 0-19 must be zero), got 0x" + Integer.toHexString(baseAddress));
        }
    }

    public int encode() {
        return baseAddress;
    }

    public static PvrObjectBufferAddress decode(int value) {
        return new PvrObjectBufferAddress(value & FIELD_MASK);
    }
}
