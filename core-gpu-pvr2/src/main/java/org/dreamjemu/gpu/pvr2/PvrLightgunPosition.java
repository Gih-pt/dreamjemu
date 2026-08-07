package org.dreamjemu.gpu.pvr2;

/**
 * The current light gun position, as tracked by the PVR2 against the
 * raster beam.
 *
 * <p>Source: "Guide to the PowerVR-chip of the Dreamcast" by Lars Olsson,
 * v0.51 (hitmen.c02.at/files/docs/dc/powervr-reg.txt), register
 * {@code a05f80c4 (gun_pos)}:
 *
 * <pre>
 * bits 31-26 : ??? (documented only as "???" - not modeled)
 * bits 25-16 : vpos (vertical position of light gun)
 * bits 15-10 : n/a
 * bits 9-0   : hpos (horizontal position of light gun)
 * </pre>
 *
 * <p>This is a read-only hardware status register (the PVR2 reports
 * where it detected the gun's signal during the last raster pass), so
 * only {@link #decode} is provided — there's nothing meaningful to
 * "encode" here as a value to write.
 */
public record PvrLightgunPosition(int verticalPosition, int horizontalPosition) {

    /** P2 (uncached) address of this register. */
    public static final int REGISTER_ADDRESS = 0xA05F80C4;

    private static final int VPOS_SHIFT = 16;
    private static final int VPOS_MASK = 0x3FF;
    private static final int HPOS_MASK = 0x3FF;

    public static PvrLightgunPosition decode(int value) {
        int verticalPosition = (value >>> VPOS_SHIFT) & VPOS_MASK;
        int horizontalPosition = value & HPOS_MASK;
        return new PvrLightgunPosition(verticalPosition, horizontalPosition);
    }
}
