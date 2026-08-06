package org.dreamjemu.gpu.pvr2;

/**
 * The 5 polygon list types the Tile Accelerator sorts incoming geometry
 * into. Each has its own Object Pointer Buffer allocation
 * (see {@link PvrObjectPointerBufferConfig}).
 *
 * <p>Source: "Guide to the PowerVR-chip of the Dreamcast" by Lars Olsson,
 * v0.51 (hitmen.c02.at/files/docs/dc/powervr-reg.txt), register
 * {@code a05f8140 (ta_opb_cfg)}, which names exactly these 5 list types:
 * {@code opaquepoly}, {@code opaquemod}, {@code transpoly},
 * {@code transmod}, and {@code punch-through}.
 *
 * <p>{@link #fieldValue()}/{@link #fromFieldValue} added later from a
 * second, independent source that also names and numbers these same 5
 * types: Sega's own "Dreamcast/Dev.Box System Architecture" manual
 * (segaretro.org/images/7/78/DreamcastDevBoxSystemArchitecture.pdf),
 * §3.7.4.4.1 "Para Control", the {@code List type} table (HOLLY2 column —
 * this is the 3-bit encoding that includes Punch Through; the older
 * HOLLY1 chip only had the first 4 and used a 2-bit field instead):
 *
 * <pre>
 * 0: Opaque
 * 1: Opaque Modifier Volume
 * 2: Translucent
 * 3: Translucent Modifier Volume
 * 4: Punch Through (HOLLY2)
 * </pre>
 */
public enum TileAcceleratorListType {
    OPAQUE_POLYGONS(0),
    OPAQUE_MODIFIERS(1),
    TRANSLUCENT_POLYGONS(2),
    TRANSLUCENT_MODIFIERS(3),
    PUNCH_THROUGH_POLYGONS(4);

    private final int fieldValue;

    TileAcceleratorListType(int fieldValue) {
        this.fieldValue = fieldValue;
    }

    public int fieldValue() {
        return fieldValue;
    }

    public static TileAcceleratorListType fromFieldValue(int fieldValue) {
        for (TileAcceleratorListType listType : values()) {
            if (listType.fieldValue == fieldValue) {
                return listType;
            }
        }
        throw new IllegalArgumentException("TA list type field must be 0-4, got " + fieldValue);
    }
}

