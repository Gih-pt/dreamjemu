package org.dreamjemu.maple;

/**
 * {@code SB_MDST} — starts a software-initiated Maple-DMA transfer when
 * written, and reports transfer-in-progress status when read.
 *
 * <p>Source: Sega's "Dreamcast/Dev.Box System Architecture" manual,
 * §8.4.1.2 "Maple Peripheral Interface":
 *
 * <pre>
 * bits 31-1 : Reserved
 * bit  0    : Maple-DMA start/status (default = 0)
 *             write 0 = ignored,                 write 1 = Maple-DMA start
 *             read  0 = Maple-DMA not in progress, read 1 = Maple-DMA in progress
 * </pre>
 *
 * <p>Notes from the source: writing here is only valid when
 * {@link MapleDmaTrigger#SOFTWARE} is selected in {@code SB_MDTSEL}; a 1
 * must not be written while {@code SB_MDEN} has Maple-DMA disabled.
 * Since the write and read meanings of bit 0 genuinely differ (a command
 * vs. a status flag, not just a value being echoed back), this class
 * exposes them as two differently-named accessors — {@link #inProgress}
 * for reads, and the boolean constructor argument represents "start
 * requested" for writes — rather than one ambiguous name.
 */
public record MapleDmaStart(boolean inProgress) {

    /** P2 (uncached) address of this register. */
    public static final int REGISTER_ADDRESS = 0x005F6C18;

    /** The value to write to trigger a software-initiated start. */
    public static int startTriggerWord() {
        return 1;
    }

    public int encode() {
        return inProgress ? 1 : 0;
    }

    public static MapleDmaStart decode(int value) {
        return new MapleDmaStart((value & 1) != 0);
    }
}
