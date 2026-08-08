package org.dreamjemu.maple;

/**
 * {@code SB_MSYS} — Maple system control: timing and rate settings for
 * the Maple interface block itself, as opposed to the DMA control
 * registers ({@code SB_MDSTAR}/{@code SB_MDTSEL}/{@code SB_MDEN}/
 * {@code SB_MDST}).
 *
 * <p>Source: Sega's "Dreamcast/Dev.Box System Architecture" manual,
 * §8.4.1.2 "Maple Peripheral Interface":
 *
 * <pre>
 * bits 31-16 : Time Out Counter (default 0x3A98; 1 = 20ns)
 * bits 15-13 : Reserved
 * bit  12    : Single Hard Trigger (0 = Automatic (default), 1 = Manual)
 * bits 11-10 : Reserved
 * bits 9-8   : Sending Rate (see {@link MapleDmaSendingRate})
 * bits 7-4   : Reserved
 * bits 3-0   : Delay Time (default 0x0; 1 = 1.3ms; values &gt;= 11 are
 *              documented as "prohibited")
 * </pre>
 *
 * <p>{@code Single Hard Trigger} and {@code Delay Time} are documented
 * as only meaningful when {@link MapleDmaTrigger#V_BLANK} is selected in
 * {@code SB_MDTSEL} — stored unconditionally here regardless, same
 * "truthful mirror of the wire format" reasoning
 * {@code TaParameterControlWord} already documents for its own
 * context-dependent fields.
 */
public record MapleSystemControl(int timeoutCounter, boolean singleHardTriggerManual, MapleDmaSendingRate sendingRate,
                                  int delayTime) {

    /** P2 (uncached) address of this register. */
    public static final int REGISTER_ADDRESS = 0x005F6C80;

    /** The documented default value: timeout 0x3A98, automatic trigger, 2Mbps, delay 0. */
    public static final MapleSystemControl DEFAULT =
            new MapleSystemControl(0x3A98, false, MapleDmaSendingRate.TWO_MBPS, 0);

    private static final int MAX_DELAY_TIME = 10; // "11 or higher ... is prohibited"
    private static final int TIMEOUT_SHIFT = 16;
    private static final int SINGLE_HARD_TRIGGER_BIT = 12;
    private static final int SENDING_RATE_SHIFT = 8;
    private static final int SENDING_RATE_MASK = 0b11;

    public MapleSystemControl {
        if (timeoutCounter < 0 || timeoutCounter > 0xFFFF) {
            throw new IllegalArgumentException("timeoutCounter must be 0-65535, got " + timeoutCounter);
        }
        if (delayTime < 0 || delayTime > MAX_DELAY_TIME) {
            throw new IllegalArgumentException(
                    "delayTime must be 0-" + MAX_DELAY_TIME + " (11+ is documented as prohibited), got " + delayTime);
        }
    }

    /** {@link #timeoutCounter}, converted to nanoseconds (1 count = 20ns). */
    public long timeoutNanoseconds() {
        return timeoutCounter * 20L;
    }

    /** {@link #delayTime}, converted to microseconds (1 count = 1.3ms = 1300us). */
    public long delayTimeMicroseconds() {
        return delayTime * 1300L;
    }

    public int encode() {
        int value = (timeoutCounter & 0xFFFF) << TIMEOUT_SHIFT;
        if (singleHardTriggerManual) {
            value |= 1 << SINGLE_HARD_TRIGGER_BIT;
        }
        value |= (sendingRate.fieldValue() & SENDING_RATE_MASK) << SENDING_RATE_SHIFT;
        value |= delayTime & 0xF;
        return value;
    }

    public static MapleSystemControl decode(int value) {
        int timeoutCounter = (value >>> TIMEOUT_SHIFT) & 0xFFFF;
        boolean singleHardTriggerManual = ((value >>> SINGLE_HARD_TRIGGER_BIT) & 1) != 0;
        MapleDmaSendingRate sendingRate =
                MapleDmaSendingRate.fromFieldValue((value >>> SENDING_RATE_SHIFT) & SENDING_RATE_MASK);
        int delayTime = value & 0xF;
        return new MapleSystemControl(timeoutCounter, singleHardTriggerManual, sendingRate, delayTime);
    }
}
