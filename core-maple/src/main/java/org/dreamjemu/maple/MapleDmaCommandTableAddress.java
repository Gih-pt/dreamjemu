package org.dreamjemu.maple;

/**
 * {@code SB_MDSTAR} — the address of the Maple-DMA command table (a
 * sequence of {@link MapleTransferDescriptorHeader}s followed by their
 * outgoing frame data) in system memory.
 *
 * <p>Source: Sega's "Dreamcast/Dev.Box System Architecture" manual
 * (segaretro.org/images/7/78/DreamcastDevBoxSystemArchitecture.pdf),
 * §8.4.1.2 "Maple Peripheral Interface":
 *
 * <pre>
 * bit  31-29 : 000 (fixed)
 * bits 28-5  : Maple-DMA command table address
 * bits 4-0   : Reserved
 * </pre>
 *
 * <p>The reserved low 5 bits mean the address must be 32-byte aligned —
 * matching §2.6.8's worked example, which gives the settable range as
 * {@code 0x0C000000} to {@code 0x0FFFFFE0} (note the trailing {@code E0},
 * not {@code FF} — the last 5 bits are never set). "Bit 31-29: 000
 * (fixed)" further restricts the usable address range to the P1/P2
 * 0x0C000000-0x0FFFFFFF window (system RAM), consistent with that same
 * worked example.
 */
public record MapleDmaCommandTableAddress(int address) {

    /** P2 (uncached) address of this register. */
    public static final int REGISTER_ADDRESS = 0x005F6C04;

    private static final int FIELD_SHIFT = 5;
    private static final int FIELD_MASK = 0xFFFFFF; // 24 bits, occupying register bits 28-5
    private static final int ALIGNMENT_MASK = 0x1F;

    public MapleDmaCommandTableAddress {
        if ((address & ALIGNMENT_MASK) != 0) {
            throw new IllegalArgumentException(
                    "address must be 32-byte aligned, got 0x" + Integer.toHexString(address));
        }
        if ((address & 0xE0000000) != 0) {
            throw new IllegalArgumentException(
                    "address's bits 31-29 must be 000 (fixed), got 0x" + Integer.toHexString(address));
        }
        if (((address >>> FIELD_SHIFT) & ~FIELD_MASK) != 0) {
            throw new IllegalArgumentException(
                    "address's bits 28-5 field must fit in 24 bits, got 0x" + Integer.toHexString(address));
        }
    }

    public int encode() {
        return address;
    }

    public static MapleDmaCommandTableAddress decode(int value) {
        return new MapleDmaCommandTableAddress(value);
    }
}
