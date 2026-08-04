package org.dreamjemu.gpu.pvr2;

/**
 * The Object Pointer Buffer allocation size for one {@link TileAcceleratorListType}
 * — a 2-bit field repeated 5 times (once per list type) in the
 * {@code ta_opb_cfg} register.
 *
 * <p>Source: "Guide to the PowerVR-chip of the Dreamcast" by Lars Olsson,
 * v0.51 (hitmen.c02.at/files/docs/dc/powervr-reg.txt), register
 * {@code a05f8140 (ta_opb_cfg)}. The source documents each of the 5
 * per-list-type fields with the identical set of 4 values shown here
 * (using {@code opaquepoly}'s wording verbatim as the canonical one):
 *
 * <pre>
 * 0: size_0  - disabled
 * 1: size_8  -  7 Object Pointers + 1 List Pointer
 * 2: size_16 - 15 Object Pointers + 1 List Pointer
 * 3: size_32 - 31 Object Pointers + 1 List Pointer
 * </pre>
 */
public enum ObjectPointerBufferSize {
    DISABLED(0, 0),
    SIZE_8(1, 7),
    SIZE_16(2, 15),
    SIZE_32(3, 31);

    private final int fieldValue;
    private final int objectPointerCount;

    ObjectPointerBufferSize(int fieldValue, int objectPointerCount) {
        this.fieldValue = fieldValue;
        this.objectPointerCount = objectPointerCount;
    }

    public int fieldValue() {
        return fieldValue;
    }

    /** How many Object Pointers this allocation holds, not counting the trailing List Pointer. */
    public int objectPointerCount() {
        return objectPointerCount;
    }

    public static ObjectPointerBufferSize fromFieldValue(int fieldValue) {
        for (ObjectPointerBufferSize size : values()) {
            if (size.fieldValue == fieldValue) {
                return size;
            }
        }
        throw new IllegalArgumentException("Object Pointer Buffer size field must be 0-3, got " + fieldValue);
    }
}
