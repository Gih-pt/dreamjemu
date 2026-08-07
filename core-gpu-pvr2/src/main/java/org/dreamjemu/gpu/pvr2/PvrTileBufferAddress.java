package org.dreamjemu.gpu.pvr2;

/**
 * The location of the Tile Buffer in VRAM used when the PVR is
 * rendering a scene.
 *
 * <p>Source: "Guide to the PowerVR-chip of the Dreamcast" by Lars Olsson,
 * v0.51 (hitmen.c02.at/files/docs/dc/powervr-reg.txt), register
 * {@code a05f802c (tilebuf_addr)}:
 *
 * <pre>
 * bits 31-24 : n/a
 * bits 23-0  : addr (64-bit aligned)
 * </pre>
 *
 * <p>"64-bit aligned" means the address must be a multiple of 8 bytes —
 * enforced at construction rather than silently masked away.
 */
public record PvrTileBufferAddress(int address) {

    /** P2 (uncached) address of this register. */
    public static final int REGISTER_ADDRESS = 0xA05F802C;

    private static final int FIELD_MASK = 0xFFFFFF;
    private static final int ALIGNMENT_MASK = 0x7;

    public PvrTileBufferAddress {
        if ((address & ~FIELD_MASK) != 0) {
            throw new IllegalArgumentException("address must fit in 24 bits, got 0x" + Integer.toHexString(address));
        }
        if ((address & ALIGNMENT_MASK) != 0) {
            throw new IllegalArgumentException("address must be 64-bit (8-byte) aligned, got 0x" + Integer.toHexString(address));
        }
    }

    public int encode() {
        return address;
    }

    public static PvrTileBufferAddress decode(int value) {
        return new PvrTileBufferAddress(value & FIELD_MASK);
    }
}
