package org.dreamjemu.maple;

/**
 * {@code SB_MDTSEL} — selects what initiates a Maple-DMA transfer.
 *
 * <p>Source: Sega's "Dreamcast/Dev.Box System Architecture" manual,
 * §8.4.1.2 "Maple Peripheral Interface":
 *
 * <pre>
 * bits 31-1 : Reserved
 * bit  0    : Maple-DMA Trigger select
 *             0 = Software initiation (default) - via SB_MDST
 *             1 = V-Blank initiation - automatically one line before
 *                 the start of screen display (V-Blank Out)
 * </pre>
 */
public record MapleDmaTriggerSelect(MapleDmaTrigger trigger) {

    /** P2 (uncached) address of this register. */
    public static final int REGISTER_ADDRESS = 0x005F6C10;

    public int encode() {
        return trigger.fieldValue();
    }

    public static MapleDmaTriggerSelect decode(int value) {
        return new MapleDmaTriggerSelect(MapleDmaTrigger.fromFieldValue(value & 1));
    }
}
