package org.dreamjemu.gpu.pvr2;

/**
 * The Object Pointer Buffer configuration register: the growth direction
 * of the buffer, plus a per-{@link TileAcceleratorListType}
 * {@link ObjectPointerBufferSize} allocation.
 *
 * <p>Source: "Guide to the PowerVR-chip of the Dreamcast" by Lars Olsson,
 * v0.51 (hitmen.c02.at/files/docs/dc/powervr-reg.txt), register
 * {@code a05f8140 (ta_opb_cfg)}:
 *
 * <pre>
 * bits 31-21 : n/a
 * bit  20    : opbdir (0 = buffer grows upward in VRAM, 1 = grows downward)
 * bits 19-18 : n/a
 * bits 17-16 : punch-through
 * bits 15-14 : n/a
 * bits 13-12 : transmod (translucent modifiers)
 * bits 11-10 : n/a
 * bits 9-8   : transpoly (translucent polygons)
 * bits 7-6   : n/a
 * bits 5-4   : opaquemod (opaque modifiers)
 * bits 3-2   : n/a (bit 2 is documented only as "???" - not modeled)
 * bits 1-0   : opaquepoly (opaque polygons)
 * </pre>
 */
public record PvrObjectPointerBufferConfig(boolean growsDownward, ObjectPointerBufferSize opaquePolygons,
                                            ObjectPointerBufferSize opaqueModifiers,
                                            ObjectPointerBufferSize translucentPolygons,
                                            ObjectPointerBufferSize translucentModifiers,
                                            ObjectPointerBufferSize punchThroughPolygons) {

    /** P2 (uncached) address of this register. */
    public static final int REGISTER_ADDRESS = 0xA05F8140;

    private static final int GROWS_DOWNWARD_BIT = 20;
    private static final int PUNCH_THROUGH_SHIFT = 16;
    private static final int TRANSLUCENT_MODIFIERS_SHIFT = 12;
    private static final int TRANSLUCENT_POLYGONS_SHIFT = 8;
    private static final int OPAQUE_MODIFIERS_SHIFT = 4;
    private static final int OPAQUE_POLYGONS_SHIFT = 0;
    private static final int FIELD_MASK = 0b11;

    /** Looks up the configured size for a given list type, without the caller needing a switch on the 5 fields. */
    public ObjectPointerBufferSize sizeFor(TileAcceleratorListType listType) {
        return switch (listType) {
            case OPAQUE_POLYGONS -> opaquePolygons;
            case OPAQUE_MODIFIERS -> opaqueModifiers;
            case TRANSLUCENT_POLYGONS -> translucentPolygons;
            case TRANSLUCENT_MODIFIERS -> translucentModifiers;
            case PUNCH_THROUGH_POLYGONS -> punchThroughPolygons;
        };
    }

    public int encode() {
        int value = 0;
        if (growsDownward) {
            value |= 1 << GROWS_DOWNWARD_BIT;
        }
        value |= punchThroughPolygons.fieldValue() << PUNCH_THROUGH_SHIFT;
        value |= translucentModifiers.fieldValue() << TRANSLUCENT_MODIFIERS_SHIFT;
        value |= translucentPolygons.fieldValue() << TRANSLUCENT_POLYGONS_SHIFT;
        value |= opaqueModifiers.fieldValue() << OPAQUE_MODIFIERS_SHIFT;
        value |= opaquePolygons.fieldValue() << OPAQUE_POLYGONS_SHIFT;
        return value;
    }

    public static PvrObjectPointerBufferConfig decode(int value) {
        boolean growsDownward = ((value >>> GROWS_DOWNWARD_BIT) & 1) != 0;
        ObjectPointerBufferSize punchThrough =
                ObjectPointerBufferSize.fromFieldValue((value >>> PUNCH_THROUGH_SHIFT) & FIELD_MASK);
        ObjectPointerBufferSize translucentModifiers =
                ObjectPointerBufferSize.fromFieldValue((value >>> TRANSLUCENT_MODIFIERS_SHIFT) & FIELD_MASK);
        ObjectPointerBufferSize translucentPolygons =
                ObjectPointerBufferSize.fromFieldValue((value >>> TRANSLUCENT_POLYGONS_SHIFT) & FIELD_MASK);
        ObjectPointerBufferSize opaqueModifiers =
                ObjectPointerBufferSize.fromFieldValue((value >>> OPAQUE_MODIFIERS_SHIFT) & FIELD_MASK);
        ObjectPointerBufferSize opaquePolygons =
                ObjectPointerBufferSize.fromFieldValue((value >>> OPAQUE_POLYGONS_SHIFT) & FIELD_MASK);
        return new PvrObjectPointerBufferConfig(
                growsDownward, opaquePolygons, opaqueModifiers, translucentPolygons, translucentModifiers, punchThrough);
    }
}
