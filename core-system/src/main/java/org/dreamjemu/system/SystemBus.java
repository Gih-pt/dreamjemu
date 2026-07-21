package org.dreamjemu.system;

import java.util.ArrayList;
import java.util.List;

/**
 * Routes CPU-facing addresses to the correct {@link MemoryRegion}, handling
 * the SH-4's cache-area address mirroring (P0–P3: the top 3 address bits
 * select a cache mode but don't change which physical device is targeted)
 * and the main RAM mirrors within the physical address space.
 *
 * See {@link DreamcastAddressMap} for the ranges this currently understands.
 * Anything outside those ranges resolves to a single shared
 * {@link UnmappedRegion} covering the rest of the address space, so
 * bring-up code can run (reading 0 / ignoring writes) before every
 * peripheral module exists.
 */
public final class SystemBus implements Bus {

    private static final class MappedRange {
        final long start; // physical, inclusive
        final long end;   // physical, exclusive
        final MemoryRegion region;
        final long regionOffsetBase; // subtracted from physical address, then modulo region.size()

        MappedRange(long start, long end, MemoryRegion region, long regionOffsetBase) {
            this.start = start;
            this.end = end;
            this.region = region;
            this.regionOffsetBase = regionOffsetBase;
        }

        boolean contains(long physicalAddress) {
            return physicalAddress >= start && physicalAddress < end;
        }
    }

    private final List<MappedRange> ranges = new ArrayList<>();
    private final MemoryRegion unmapped;

    public final MemoryRegion mainRam;
    public final MemoryRegion bootRom;
    public final MemoryRegion flashRom;
    public final MemoryRegion vram;
    public final MemoryRegion aicaRam;

    public SystemBus() {
        this.mainRam = new RamRegion("Main RAM", (int) DreamcastAddressMap.MAIN_RAM_SIZE);
        this.bootRom = new UnmappedRegion("Boot ROM (HLE placeholder)", DreamcastAddressMap.BOOT_ROM_SIZE);
        this.flashRom = new UnmappedRegion("Flash ROM (placeholder)", DreamcastAddressMap.FLASH_ROM_SIZE);
        this.vram = new UnmappedRegion("VRAM (owned by core-gpu-pvr2, not yet implemented)", DreamcastAddressMap.VRAM_SIZE);
        this.aicaRam = new UnmappedRegion("AICA RAM (owned by core-aica, not yet implemented)", DreamcastAddressMap.AICA_RAM_SIZE);
        this.unmapped = new UnmappedRegion("Unmapped", 0); // size unused for the catch-all

        ranges.add(new MappedRange(
                DreamcastAddressMap.BOOT_ROM_BASE,
                DreamcastAddressMap.BOOT_ROM_BASE + DreamcastAddressMap.BOOT_ROM_SIZE,
                bootRom, DreamcastAddressMap.BOOT_ROM_BASE));

        ranges.add(new MappedRange(
                DreamcastAddressMap.FLASH_ROM_BASE,
                DreamcastAddressMap.FLASH_ROM_BASE + DreamcastAddressMap.FLASH_ROM_SIZE,
                flashRom, DreamcastAddressMap.FLASH_ROM_BASE));

        ranges.add(new MappedRange(
                DreamcastAddressMap.AICA_RAM_BASE,
                DreamcastAddressMap.AICA_RAM_BASE + DreamcastAddressMap.AICA_RAM_SIZE,
                aicaRam, DreamcastAddressMap.AICA_RAM_BASE));

        ranges.add(new MappedRange(
                DreamcastAddressMap.VRAM_BASE,
                DreamcastAddressMap.VRAM_BASE + DreamcastAddressMap.VRAM_SIZE,
                vram, DreamcastAddressMap.VRAM_BASE));

        // Main RAM: one MappedRange spanning all four mirrors; offset is taken
        // modulo the real RAM size when translating (see resolveOffset).
        ranges.add(new MappedRange(
                DreamcastAddressMap.MAIN_RAM_BASE,
                DreamcastAddressMap.MAIN_RAM_BASE + DreamcastAddressMap.MAIN_RAM_MIRROR_SPAN,
                mainRam, DreamcastAddressMap.MAIN_RAM_BASE));
    }

    private long toPhysical(long address) {
        return address & DreamcastAddressMap.PHYSICAL_ADDRESS_MASK;
    }

    private MappedRange findRange(long physicalAddress) {
        for (MappedRange range : ranges) {
            if (range.contains(physicalAddress)) {
                return range;
            }
        }
        return null;
    }

    private MemoryRegion resolveRegion(long physicalAddress, long[] offsetOut) {
        MappedRange range = findRange(physicalAddress);
        if (range == null) {
            offsetOut[0] = physicalAddress;
            return unmapped;
        }
        long rawOffset = physicalAddress - range.regionOffsetBase;
        // Wrap into the region's real size so multi-mirror ranges (like main RAM)
        // resolve correctly regardless of which mirror was accessed.
        offsetOut[0] = range.region.size() > 0 ? (rawOffset % range.region.size()) : rawOffset;
        return range.region;
    }

    @Override
    public byte read8(long address) {
        long[] off = new long[1];
        MemoryRegion region = resolveRegion(toPhysical(address), off);
        return region.read8(off[0]);
    }

    @Override
    public short read16(long address) {
        long[] off = new long[1];
        MemoryRegion region = resolveRegion(toPhysical(address), off);
        return region.read16(off[0]);
    }

    @Override
    public int read32(long address) {
        long[] off = new long[1];
        MemoryRegion region = resolveRegion(toPhysical(address), off);
        return region.read32(off[0]);
    }

    @Override
    public long read64(long address) {
        long[] off = new long[1];
        MemoryRegion region = resolveRegion(toPhysical(address), off);
        return region.read64(off[0]);
    }

    @Override
    public void write8(long address, byte value) {
        long[] off = new long[1];
        MemoryRegion region = resolveRegion(toPhysical(address), off);
        region.write8(off[0], value);
    }

    @Override
    public void write16(long address, short value) {
        long[] off = new long[1];
        MemoryRegion region = resolveRegion(toPhysical(address), off);
        region.write16(off[0], value);
    }

    @Override
    public void write32(long address, int value) {
        long[] off = new long[1];
        MemoryRegion region = resolveRegion(toPhysical(address), off);
        region.write32(off[0], value);
    }

    @Override
    public void write64(long address, long value) {
        long[] off = new long[1];
        MemoryRegion region = resolveRegion(toPhysical(address), off);
        region.write64(off[0], value);
    }
}
