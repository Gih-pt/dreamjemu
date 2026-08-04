package org.dreamjemu.gpu.pvr2;

/**
 * The PVR2 reset control register: three independent reset lines for the
 * VRAM bus, the PVR core, and the Tile Accelerator.
 *
 * <p>Source: "Guide to the PowerVR-chip of the Dreamcast" by Lars Olsson,
 * v0.51 (hitmen.c02.at/files/docs/dc/powervr-reg.txt), register
 * {@code a05f8008 (reset)}:
 *
 * <pre>
 * bits 31-3 : n/a
 * bit  2    : bus  (0 = normal, 1 = reset VRAM bus)
 * bit  1    : PVR  (0 = normal, 1 = reset PVR)
 * bit  0    : TA   (0 = normal, 1 = reset Tile Accelerator)
 * </pre>
 */
public record PvrSoftwareReset(boolean resetVramBus, boolean resetPvrCore, boolean resetTileAccelerator) {

    /** P2 (uncached) address of this register. */
    public static final int REGISTER_ADDRESS = 0xA05F8008;

    private static final int VRAM_BUS_BIT = 0x4;
    private static final int PVR_CORE_BIT = 0x2;
    private static final int TILE_ACCELERATOR_BIT = 0x1;

    /** A value with every reset line held normal (inactive). */
    public static PvrSoftwareReset normal() {
        return new PvrSoftwareReset(false, false, false);
    }

    public int encode() {
        int value = 0;
        if (resetVramBus) {
            value |= VRAM_BUS_BIT;
        }
        if (resetPvrCore) {
            value |= PVR_CORE_BIT;
        }
        if (resetTileAccelerator) {
            value |= TILE_ACCELERATOR_BIT;
        }
        return value;
    }

    public static PvrSoftwareReset decode(int value) {
        return new PvrSoftwareReset(
                (value & VRAM_BUS_BIT) != 0,
                (value & PVR_CORE_BIT) != 0,
                (value & TILE_ACCELERATOR_BIT) != 0);
    }
}
