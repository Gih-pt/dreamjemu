package org.dreamjemu.gpu.pvr2;

/**
 * The {@code Col_Type} field (bits 5-4 of Obj Control) — the format of
 * the Shading Color data that follows in a Global/Vertex Parameter.
 *
 * <p>Source: Sega's "Dreamcast/Dev.Box System Architecture" manual
 * (segaretro.org/images/7/78/DreamcastDevBoxSystemArchitecture.pdf),
 * §3.7.4.4.3 "Obj Control", the {@code Col_Type} table:
 *
 * <pre>
 * 0: Packed Color     - 8-bit values for each of A, R, G, and B
 * 1: Floating Color   - 32-bit floating-point values for each of A, R, G, and B
 * 2: Intensity Mode 1 - Face Color specified by the immediately preceding Global Parameters
 * 3: Intensity Mode 2 - reuses the previous Mode-1 Face Color, saving the transfer
 * </pre>
 */
public enum TaColorType {
    PACKED_COLOR(0),
    FLOATING_COLOR(1),
    INTENSITY_MODE_1(2),
    INTENSITY_MODE_2(3);

    private final int fieldValue;

    TaColorType(int fieldValue) {
        this.fieldValue = fieldValue;
    }

    public int fieldValue() {
        return fieldValue;
    }

    public static TaColorType fromFieldValue(int fieldValue) {
        for (TaColorType type : values()) {
            if (type.fieldValue == fieldValue) {
                return type;
            }
        }
        throw new IllegalArgumentException("Col_Type field must be 0-3, got " + fieldValue);
    }
}
