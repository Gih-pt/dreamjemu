package org.dreamjemu.maple;

/**
 * {@code SB_MDEN} — the Maple-DMA enable register. Must be set before
 * {@code SB_MDST} can start a transfer.
 *
 * <p>Source: Sega's "Dreamcast/Dev.Box System Architecture" manual,
 * §8.4.1.2 "Maple Peripheral Interface":
 *
 * <pre>
 * bits 31-1 : Reserved
 * bit  0    : Maple-DMA enable (default = 0)
 *             write 0 = Abort Maple-DMA, write 1 = Enable
 *             read  0 = Disabled,        read  1 = Enabled
 * </pre>
 *
 * <p>Notes from the source: transmission/reception is not performed
 * unless this bit is set; DMA is forcibly terminated if 0 is written
 * here while a transfer is in progress.
 */
public record MapleDmaEnable(boolean enabled) {

    /** P2 (uncached) address of this register. */
    public static final int REGISTER_ADDRESS = 0x005F6C14;

    public int encode() {
        return enabled ? 1 : 0;
    }

    public static MapleDmaEnable decode(int value) {
        return new MapleDmaEnable((value & 1) != 0);
    }
}
