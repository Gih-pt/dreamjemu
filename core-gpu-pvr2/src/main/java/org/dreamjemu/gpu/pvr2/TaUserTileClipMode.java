package org.dreamjemu.gpu.pvr2;

/**
 * The {@code User_Clip} field (bits 17-16 of Group Control) — how the
 * User Tile Clipping area (set separately via the {@code User Tile Clip}
 * Control Parameter) applies to this object. Only meaningful when
 * {@code Group_En} is set.
 *
 * <p>Source: Sega's "Dreamcast/Dev.Box System Architecture" manual
 * (segaretro.org/images/7/78/DreamcastDevBoxSystemArchitecture.pdf),
 * §3.7.4.4.2 "Group Control", the {@code User_Clip} table:
 *
 * <pre>
 * 0: Disable
 * 1: Reserved
 * 2: Inside enable
 * 3: Outside enable
 * </pre>
 */
public enum TaUserTileClipMode {
    DISABLED(0),
    RESERVED(1),
    INSIDE_ENABLE(2),
    OUTSIDE_ENABLE(3);

    private final int fieldValue;

    TaUserTileClipMode(int fieldValue) {
        this.fieldValue = fieldValue;
    }

    public int fieldValue() {
        return fieldValue;
    }

    public static TaUserTileClipMode fromFieldValue(int fieldValue) {
        for (TaUserTileClipMode mode : values()) {
            if (mode.fieldValue == fieldValue) {
                return mode;
            }
        }
        throw new IllegalArgumentException("User_Clip field must be 0-3, got " + fieldValue);
    }
}
