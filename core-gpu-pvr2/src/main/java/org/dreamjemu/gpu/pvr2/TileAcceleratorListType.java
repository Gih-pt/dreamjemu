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
 */
public enum TileAcceleratorListType {
    OPAQUE_POLYGONS,
    OPAQUE_MODIFIERS,
    TRANSLUCENT_POLYGONS,
    TRANSLUCENT_MODIFIERS,
    PUNCH_THROUGH_POLYGONS
}
