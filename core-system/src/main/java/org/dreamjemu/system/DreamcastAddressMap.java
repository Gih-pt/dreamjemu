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

    // --- PVR2 (Holly) register block (owned by core-gpu-pvr2's PvrRegisters) ---
    // Confirmed against two independent authoritative sources that agree exactly (KallistiOS's
    // and Flycast's own pvr_regs.h): base 0x005F8000, size 0x8000. Sits in the gap between
    // FLASH_ROM (ends 0x0022_0000) and AICA_RAM_BASE (0x0080_0000), so it needs no separate
    // "unmapped gap" adjustment. Only SPG_STATUS (offset 0x10C) is actually modeled yet — see
    // PvrRegisters' Javadoc for why, and for the real Sonic Adventure spin-wait that made this
    // necessary.
    public static final long PVR2_REGISTERS_BASE = 0x005F_8000L;
    public static final long PVR2_REGISTERS_SIZE = 0x0000_8000L; // 32 KB

    // --- Holly "System Control Reg." block (owned by core-gpu-pvr2's HollySystemRegisters) ---
    // Confirmed against the official Sega Dev.Box System Architecture manual's own physical
    // memory map (Table 2-1): base 0x005F6800, size 0x200. A genuinely separate named region
    // from PVR2_REGISTERS above (the manual lists "System Control Reg." and "TA/PVR Core Reg."
    // as distinct blocks), sitting in the same FLASH_ROM/AICA_RAM_BASE gap. Only SB_ISTNRM's
    // VBLANK_BEGIN bit is actually modeled yet — see HollySystemRegisters' Javadoc for why, and
    // for the real Sonic Adventure spin-wait that made this necessary.
    public static final long HOLLY_SYSTEM_REGISTERS_BASE = 0x005F_6800L;
    public static final long HOLLY_SYSTEM_REGISTERS_SIZE = 0x0000_0200L; // 512 bytes

    // --- Main system RAM ---
    // Physical range 0x0C000000-0x0FFFFFFF is four contiguous 16MB mirrors of
    // the same 16MB of RAM; SystemBus maps all four onto the same RamRegion.
    public static final long MAIN_RAM_BASE = 0x0C00_0000L;
    public static final long MAIN_RAM_SIZE = 0x0100_0000L; // 16 MB
    public static final long MAIN_RAM_MIRROR_SPAN = 0x0400_0000L; // covers all 4 mirrors

    /** Highest physical address this initial map accounts for; above this, everything is unmapped. */
    public static final long MAPPED_REGION_END = MAIN_RAM_BASE + MAIN_RAM_MIRROR_SPAN;
}
