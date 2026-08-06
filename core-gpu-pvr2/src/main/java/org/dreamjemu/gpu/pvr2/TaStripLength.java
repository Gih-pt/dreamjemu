package org.dreamjemu.gpu.pvr2;

/**
 * The {@code Strip_Len} field (bits 19-18 of Group Control) — how many
 * pieces a triangle strip is partitioned into. Only meaningful when
 * {@code Group_En} is set.
 *
 * <p>Source: Sega's "Dreamcast/Dev.Box System Architecture" manual
 * (segaretro.org/images/7/78/DreamcastDevBoxSystemArchitecture.pdf),
 * §3.7.4.4.2 "Group Control", the {@code Strip_Len} table (the source
 * labels the count column "strip 分割数" — Japanese for "strip split
 * count" — left untranslated in the source table itself):
 *
 * <pre>
 * 0: 1 strip
 * 1: 2 strip
 * 2: 4 strip
 * 3: 6 strip
 * </pre>
 */
public enum TaStripLength {
    ONE_STRIP(0, 1),
    TWO_STRIPS(1, 2),
    FOUR_STRIPS(2, 4),
    SIX_STRIPS(3, 6);

    private final int fieldValue;
    private final int stripCount;

    TaStripLength(int fieldValue, int stripCount) {
        this.fieldValue = fieldValue;
        this.stripCount = stripCount;
    }

    public int fieldValue() {
        return fieldValue;
    }

    public int stripCount() {
        return stripCount;
    }

    public static TaStripLength fromFieldValue(int fieldValue) {
        for (TaStripLength length : values()) {
            if (length.fieldValue == fieldValue) {
                return length;
            }
        }
        throw new IllegalArgumentException("Strip_Len field must be 0-3, got " + fieldValue);
    }
}
