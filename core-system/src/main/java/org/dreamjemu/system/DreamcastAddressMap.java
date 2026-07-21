package org.dreamjemu.system;

/**
 * Physical address ranges for the Dreamcast's memory map, as seen by the
 * SH-4 after cache-mirror bits are masked off (see {@link SystemBus} for
 * that translation).
 *
 * These addresses and sizes describe publicly documented Dreamcast system
 * architecture (the same information used by the wider open-source
 * Dreamcast emulation community); nothing here embeds or requires any
 * copyrighted Sega file (BIOS/firmware dump, etc.) — see /docs/STATUS.md
 * and /README.md for the project's no-original-files stance.
 *
 * This is a deliberately partial first pass (see /docs/ROADMAP.md Phase 1):
 * it covers boot ROM, flash, main RAM (with its mirrors), and reserves
 * placeholder ranges for VRAM/AICA so core-gpu-pvr2 and core-aica have
 * something to attach to later. Peripheral register blocks (Holly/PVR2,
 * Maple, GD-ROM/G1, AICA registers, etc.) are intentionally not broken out
 * individually yet — they currently fall inside the generic "unmapped"
 * catch-all handled by {@link SystemBus}, and should be split into their
 * own precise ranges as each peripheral module is implemented.
 */
public final class DreamcastAddressMap {

    private DreamcastAddressMap() {
    }

    /** Mask applied to strip the SH-4's cache-area select bits (P0–P3 mirrors) before lookup. */
    public static final long PHYSICAL_ADDRESS_MASK = 0x1FFFFFFFL;

    // --- Boot ROM (HLE placeholder — never a real BIOS dump; see README.md) ---
    public static final long BOOT_ROM_BASE = 0x0000_0000L;
    public static final long BOOT_ROM_SIZE = 0x0020_0000L; // 2 MB

    // --- Flash ROM (system settings; also HLE/placeholder, no original file required) ---
    public static final long FLASH_ROM_BASE = 0x0020_0000L;
    public static final long FLASH_ROM_SIZE = 0x0002_0000L; // 128 KB

    // --- AICA sound RAM (owned by core-aica once implemented; placeholder for now) ---
    public static final long AICA_RAM_BASE = 0x0080_0000L;
    public static final long AICA_RAM_SIZE = 0x0020_0000L; // 2 MB

    // --- Video RAM (owned by core-gpu-pvr2 once implemented; placeholder for now) ---
    public static final long VRAM_BASE = 0x0400_0000L;
    public static final long VRAM_SIZE = 0x0080_0000L; // 8 MB

    // --- Main system RAM ---
    // Physical range 0x0C000000-0x0FFFFFFF is four contiguous 16MB mirrors of
    // the same 16MB of RAM; SystemBus maps all four onto the same RamRegion.
    public static final long MAIN_RAM_BASE = 0x0C00_0000L;
    public static final long MAIN_RAM_SIZE = 0x0100_0000L; // 16 MB
    public static final long MAIN_RAM_MIRROR_SPAN = 0x0400_0000L; // covers all 4 mirrors

    /** Highest physical address this initial map accounts for; above this, everything is unmapped. */
    public static final long MAPPED_REGION_END = MAIN_RAM_BASE + MAIN_RAM_MIRROR_SPAN;
}
