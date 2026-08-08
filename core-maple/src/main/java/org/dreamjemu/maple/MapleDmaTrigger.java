package org.dreamjemu.maple;

/**
 * What initiates a Maple-DMA transfer — the value of {@link MapleDmaTriggerSelect}.
 *
 * <p>Source: Sega's "Dreamcast/Dev.Box System Architecture" manual,
 * §8.4.1.2 "Maple Peripheral Interface", {@code SB_MDTSEL}.
 */
public enum MapleDmaTrigger {
    /** Maple-DMA is initiated by an SH4 write to {@code SB_MDST}. Default. */
    SOFTWARE(0),
    /** Maple-DMA is initiated automatically one line before V-Blank Out. */
    V_BLANK(1);

    private final int fieldValue;

    MapleDmaTrigger(int fieldValue) {
        this.fieldValue = fieldValue;
    }

    public int fieldValue() {
        return fieldValue;
    }

    public static MapleDmaTrigger fromFieldValue(int fieldValue) {
        for (MapleDmaTrigger trigger : values()) {
            if (trigger.fieldValue == fieldValue) {
                return trigger;
            }
        }
        throw new IllegalArgumentException("SB_MDTSEL trigger field must be 0-1, got " + fieldValue);
    }
}
