package org.dreamjemu.system;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SystemBusTest {

    @Test
    void readWriteRoundTripInMainRam() {
        SystemBus bus = new SystemBus();
        long addr = DreamcastAddressMap.MAIN_RAM_BASE + 0x1234;

        bus.write32(addr, 0xDEADBEEF);
        assertEquals(0xDEADBEEF, bus.read32(addr));

        bus.write8(addr, (byte) 0x7A);
        assertEquals((byte) 0x7A, bus.read8(addr));

        bus.write16(addr, (short) 0xBEEF);
        assertEquals((short) 0xBEEF, bus.read16(addr));

        bus.write64(addr, 0x1122334455667788L);
        assertEquals(0x1122334455667788L, bus.read64(addr));
    }

    @Test
    void littleEndianByteOrder() {
        SystemBus bus = new SystemBus();
        long addr = DreamcastAddressMap.MAIN_RAM_BASE;

        bus.write32(addr, 0x11223344);
        assertEquals((byte) 0x44, bus.read8(addr));
        assertEquals((byte) 0x33, bus.read8(addr + 1));
        assertEquals((byte) 0x22, bus.read8(addr + 2));
        assertEquals((byte) 0x11, bus.read8(addr + 3));
    }

    @Test
    void mainRamMirrorsAreConsistent() {
        // Physical range 0x0C000000-0x0FFFFFFF is documented as four 16MB
        // mirrors of the same underlying RAM. Writing through one mirror
        // must be visible through the others.
        SystemBus bus = new SystemBus();
        long offset = 0x2000;

        long mirror0 = 0x0C000000L + offset;
        long mirror1 = 0x0D000000L + offset;
        long mirror2 = 0x0E000000L + offset;
        long mirror3 = 0x0F000000L + offset;

        bus.write32(mirror0, 0xCAFEBABE);

        assertEquals(0xCAFEBABE, bus.read32(mirror1));
        assertEquals(0xCAFEBABE, bus.read32(mirror2));
        assertEquals(0xCAFEBABE, bus.read32(mirror3));
    }

    @Test
    void cacheAreaBitsAreMaskedToTheSamePhysicalMemory() {
        // The SH-4's P0/P1/P2 cache-mode mirrors (address bits 31-29) must
        // resolve to the same physical device. 0x0C000000, 0x8C000000, and
        // 0xAC000000 should all reach the same RAM byte.
        SystemBus bus = new SystemBus();
        long physical = DreamcastAddressMap.MAIN_RAM_BASE + 0x100;

        bus.write32(physical, 0x600D0000);

        assertEquals(0x600D0000, bus.read32(0x80000000L | physical));
        assertEquals(0x600D0000, bus.read32(0xA0000000L | physical));
    }

    @Test
    void unmappedAddressesReadAsZeroAndIgnoreWrites() {
        SystemBus bus = new SystemBus();
        long farAwayAddress = 0x1E000000L; // outside every currently-mapped range

        // Should not throw, and should read back as zero.
        bus.write32(farAwayAddress, 0x12345678);
        assertEquals(0, bus.read32(farAwayAddress));
    }

    @Test
    void vramAndAicaRamAreReservedButNotYetImplemented() {
        // These regions exist as named placeholders so core-gpu-pvr2 / core-aica
        // have somewhere to attach real implementations later (see docs/ROADMAP.md).
        // For now they behave like any other unmapped range: read 0, ignore writes.
        SystemBus bus = new SystemBus();

        bus.write32(DreamcastAddressMap.VRAM_BASE, 0xFFFFFFFF);
        assertEquals(0, bus.read32(DreamcastAddressMap.VRAM_BASE));

        bus.write32(DreamcastAddressMap.AICA_RAM_BASE, 0xFFFFFFFF);
        assertEquals(0, bus.read32(DreamcastAddressMap.AICA_RAM_BASE));
    }
}
