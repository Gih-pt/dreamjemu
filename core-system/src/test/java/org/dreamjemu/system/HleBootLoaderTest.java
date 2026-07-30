package org.dreamjemu.system;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HleBootLoaderTest {

    @Test
    void returnsTheDocumentedEntryAddress() {
        SystemBus bus = new SystemBus();
        byte[] fileBytes = {0x01, 0x02, 0x03, 0x04};

        int entry = HleBootLoader.loadBootFile(bus, fileBytes);

        assertEquals((int) HleBootLoader.BOOT_FILE_ENTRY_ADDRESS, entry);
        assertEquals(0x8C010000L, HleBootLoader.BOOT_FILE_ENTRY_ADDRESS);
    }

    @Test
    void writesEveryByteContiguouslyStartingAtTheEntryAddress() {
        SystemBus bus = new SystemBus();
        byte[] fileBytes = {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF, 0x42};

        HleBootLoader.loadBootFile(bus, fileBytes);

        for (int i = 0; i < fileBytes.length; i++) {
            assertEquals(fileBytes[i], bus.read8(HleBootLoader.BOOT_FILE_ENTRY_ADDRESS + i));
        }
    }

    @Test
    void entryAddressResolvesToMainRamRightAfterTheIpBinLoadArea() {
        // 0x8C010000 is a P1 (cache-enabled) virtual address; once SystemBus
        // masks off the cache-select bits it must land in main RAM at
        // MAIN_RAM_BASE + 0x10000 (right after the 0x8000-byte IP.BIN area) —
        // confirmed by writing through the documented physical address and
        // reading it back through the virtual entry address.
        SystemBus bus = new SystemBus();
        long physicalEquivalent = DreamcastAddressMap.MAIN_RAM_BASE + 0x10000;

        bus.write32(physicalEquivalent, 0x600DF00D);

        assertEquals(0x600DF00D, bus.read32(HleBootLoader.BOOT_FILE_ENTRY_ADDRESS));
    }

    @Test
    void writingThroughTheEntryAddressIsVisibleThroughThePhysicalMirror() {
        SystemBus bus = new SystemBus();
        byte[] fileBytes = {0x11, 0x22, 0x33, 0x44};

        HleBootLoader.loadBootFile(bus, fileBytes);

        long physicalEquivalent = DreamcastAddressMap.MAIN_RAM_BASE + 0x10000;
        assertEquals(0x44332211, bus.read32(physicalEquivalent));
    }
}
