package org.dreamjemu.gpu.pvr2;

/**
 * The Tile Accelerator initialize trigger register.
 *
 * <p>Source: "Guide to the PowerVR-chip of the Dreamcast" by Lars Olsson,
 * v0.51 (hitmen.c02.at/files/docs/dc/powervr-reg.txt), register
 * {@code a05f8144 (ta_init)}:
 *
 * <pre>
 * bit  31   : init (0 = normal, 1 = initialize TA vertex registration parameters)
 * bits 30-0 : n/a
 * </pre>
 *
 * <p>The source describes this purely as a write trigger ("writing to this
 * register initiates..." is the same phrasing it uses for the separate
 * {@code startrender} register) and says nothing about what reading it
 * back returns. {@link #decode} is provided for API symmetry with this
 * module's other registers and is exercised by round-trip tests, but
 * unlike the read/write registers elsewhere in this class, that read-back
 * behavior is this class's own assumption, not something confirmed by the
 * source — flagged here rather than left implicit.
 */
public record PvrTaInit(boolean initialize) {

    /** P2 (uncached) address of this register. */
    public static final int REGISTER_ADDRESS = 0xA05F8144;

    private static final int INIT_BIT = 31;

    /** The word to write to trigger initialization. */
    public static int triggerWord() {
        return new PvrTaInit(true).encode();
    }

    public int encode() {
        return initialize ? (1 << INIT_BIT) : 0;
    }

    public static PvrTaInit decode(int value) {
        return new PvrTaInit(((value >>> INIT_BIT) & 1) != 0);
    }
}
