package org.dreamjemu.aica;

/**
 * A channel's {@code PlayControl} register — the primary register used to
 * start/stop a channel and select what it plays.
 *
 * <p>Source: "Yamaha AICA Sound System Hardware Reference v0.8" by yamato
 * (hitmen.c02.at/files/docs/dc/aica_v08.txt), register
 * {@code 0x0000 - PlayControl} (offset within a channel's block — see
 * {@link AicaChannelAddress}):
 *
 * <pre>
 * bits 31-16 : n/a
 * bits 15-14 : key-event
 * bits 13-11 : n/a
 * bit  10    : u (documented only as "??", not modeled here)
 * bit  9     : loop (0 = forward looping disabled, 1 = enabled)
 * bits 8-7   : format (see {@link AicaSampleFormat})
 * bits 6-0   : addr_hi (higher 7 bits of the sample start address)
 * </pre>
 *
 * <p><b>{@code key-event} is explicitly documented as uncertain by the
 * source itself</b> — it gives two competing readings for the same 2 bits
 * and says so directly:
 *
 * <pre>
 * key-event: playstate / playtrigger
 *     00: key off (??)
 *     01: is playing (??)
 *     10: key off (stop)
 *     11: key on  (play)
 *
 *     other guess:
 *     bit 14: 0 = key off, 1 = key on
 *     bit 15: 0 = "aftertouch" off, 1 = "aftertouch" = on
 * </pre>
 *
 * <p>Rather than pick one reading and present it as settled, this class
 * stores {@code keyEvent} as the raw 2-bit field and exposes
 * {@link #isKeyOn} / {@link #isAftertouch}, which implement the second
 * ("other guess") per-bit reading — the more directly actionable of the
 * two — while keeping the raw value available and this uncertainty
 * documented rather than silently resolved.
 */
public record AicaPlayControl(int keyEvent, boolean loopEnabled, AicaSampleFormat format, int sampleStartAddressHigh) {

    /** Byte offset of this register within a channel's register block. */
    public static final int REGISTER_OFFSET = 0x0000;

    private static final int KEY_EVENT_SHIFT = 14;
    private static final int KEY_EVENT_MASK = 0b11;
    private static final int LOOP_BIT = 9;
    private static final int FORMAT_SHIFT = 7;
    private static final int FORMAT_MASK = 0b11;
    private static final int ADDR_HI_MASK = 0x7F;

    private static final int KEY_ON_BIT = 0b01; // bit 14 of the register = bit 0 of the 2-bit key-event field
    private static final int AFTERTOUCH_BIT = 0b10; // bit 15 of the register = bit 1 of the 2-bit key-event field

    public AicaPlayControl {
        if ((keyEvent & ~KEY_EVENT_MASK) != 0) {
            throw new IllegalArgumentException("keyEvent must fit in 2 bits, got " + keyEvent);
        }
        if ((sampleStartAddressHigh & ~ADDR_HI_MASK) != 0) {
            throw new IllegalArgumentException("sampleStartAddressHigh must fit in 7 bits, got " + sampleStartAddressHigh);
        }
    }

    /** The "other guess" reading of {@code keyEvent} bit 0 (register bit 14): true means key on. */
    public boolean isKeyOn() {
        return (keyEvent & KEY_ON_BIT) != 0;
    }

    /** The "other guess" reading of {@code keyEvent} bit 1 (register bit 15): true means aftertouch on. */
    public boolean isAftertouch() {
        return (keyEvent & AFTERTOUCH_BIT) != 0;
    }

    public int encode() {
        int value = (keyEvent & KEY_EVENT_MASK) << KEY_EVENT_SHIFT;
        if (loopEnabled) {
            value |= 1 << LOOP_BIT;
        }
        value |= (format.fieldValue() & FORMAT_MASK) << FORMAT_SHIFT;
        value |= sampleStartAddressHigh & ADDR_HI_MASK;
        return value;
    }

    public static AicaPlayControl decode(int value) {
        int keyEvent = (value >>> KEY_EVENT_SHIFT) & KEY_EVENT_MASK;
        boolean loopEnabled = ((value >>> LOOP_BIT) & 1) != 0;
        AicaSampleFormat format = AicaSampleFormat.fromFieldValue((value >>> FORMAT_SHIFT) & FORMAT_MASK);
        int addrHi = value & ADDR_HI_MASK;
        return new AicaPlayControl(keyEvent, loopEnabled, format, addrHi);
    }
}
