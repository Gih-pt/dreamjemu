package org.dreamjemu.maple;

/**
 * The Maple bus data transfer rate between HOLLY and peripherals — the
 * {@code Sending Rate} field of {@link MapleSystemControl}.
 *
 * <p>Source: Sega's "Dreamcast/Dev.Box System Architecture" manual,
 * §8.4.1.2 "Maple Peripheral Interface", {@code SB_MSYS}, "Sending
 * Rate" table. Only settings {@code 00} and {@code 01} are documented;
 * the 2-bit field's other two values ({@code 10}/{@code 11}) aren't
 * described at all, so {@link #fromFieldValue} throws on them rather
 * than guessing a meaning.
 */
public enum MapleDmaSendingRate {
    TWO_MBPS(0b00),
    ONE_MBPS(0b01);

    private final int fieldValue;

    MapleDmaSendingRate(int fieldValue) {
        this.fieldValue = fieldValue;
    }

    public int fieldValue() {
        return fieldValue;
    }

    public static MapleDmaSendingRate fromFieldValue(int fieldValue) {
        for (MapleDmaSendingRate rate : values()) {
            if (rate.fieldValue == fieldValue) {
                return rate;
            }
        }
        throw new IllegalArgumentException(
                "Sending Rate field must be 0 (2Mbps) or 1 (1Mbps) - the source doesn't document 2 or 3, got " + fieldValue);
    }
}
