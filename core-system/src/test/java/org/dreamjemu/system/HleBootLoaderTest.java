package org.dreamjemu.system;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void bootReturnSentinelIsAllOnesAndNeverEqualToAnAddressThisProjectModels() {
        // Chosen specifically so it can never collide with a real address this project's own
        // DreamcastAddressMap models (every P0-P3 area tops out well below 0xFFFFFFFF), and so
        // it's unambiguously distinct from 0 (Sh4Cpu.pr's own uninitialized Java default) -- see
        // BOOT_RETURN_SENTINEL's own Javadoc for the full "why" (found via a real Sonic Adventure
        // run stopping at PC=0x00000000 after an RTS to an uninitialized PR).
        assertEquals(0xFFFFFFFF, HleBootLoader.BOOT_RETURN_SENTINEL);
        assertEquals(-1, HleBootLoader.BOOT_RETURN_SENTINEL,
                "0xFFFFFFFF and -1 are the same 32-bit pattern in Java's signed int -- spelling it "
                        + "as the hex literal in the constant itself is more legible at the call site "
                        + "than -1 would be, but both must denote the identical bit pattern");
        assertTrue(Integer.compareUnsigned(HleBootLoader.BOOT_RETURN_SENTINEL, (int) HleBootLoader.BOOT_FILE_ENTRY_ADDRESS) > 0,
                "the sentinel must sit above every real address this project's boot path uses, "
                        + "including the boot file's own entry address");
    }

    @Test
    void initialStackPointerIsTheTopOfMainRamsP1Mirror() {
        // 0x8D000000: DreamcastAddressMap.MAIN_RAM_BASE (0x0C000000) + MAIN_RAM_SIZE (16 MB) +
        // the same 0x80000000 P1 offset BOOT_FILE_ENTRY_ADDRESS uses -- confirmed against
        // hitmen.c02.at/files/docs/dc/memory.html's documented main-RAM window (0x8c000000 -
        // 0x8d000000) -- see the constant's own Javadoc for the full reasoning.
        assertEquals(0x8D000000, HleBootLoader.INITIAL_STACK_POINTER);
        assertTrue(Integer.compareUnsigned(HleBootLoader.INITIAL_STACK_POINTER, (int) HleBootLoader.BOOT_FILE_ENTRY_ADDRESS) > 0,
                "the initial stack pointer must sit above the boot file's own load/entry address, "
                        + "not overlap it -- a stack that immediately corrupts the code it's supposed "
                        + "to be the stack FOR would be exactly the kind of bug this constant exists to fix");
    }

    @Test
    void initialSrUnblocksInterruptsAndFullyUnmasksThem() {
        // Confirms the two bits this constant exists to fix, per its own Javadoc: BL=0
        // (interrupts not blocked) and IMASK=0000 (nothing masked) -- unlike Sh4Cpu's own real
        // hardware reset value (0x700000F0), which leaves BL=1/IMASK=1111, the state a real BIOS
        // -- not modeled by this BIOS-free HLE boot -- would already have moved past by the time
        // a real game starts running.
        int sr = HleBootLoader.INITIAL_SR;

        assertEquals(0, sr & (1 << 28), "BL (bit 28) must be clear -- interrupts not blocked");
        assertEquals(0, (sr >>> 4) & 0xF, "IMASK (bits 7-4) must be 0 -- nothing masked");
    }

    @Test
    void initialSrPreservesModeAndBankBitsFromTheRealResetValue() {
        // MD (bit 30) and RB (bit 29) are kept set, matching Sh4Cpu's own real hardware reset
        // value -- see HleBootLoader.INITIAL_SR's own Javadoc for why: the most plausible state a
        // real BIOS would still leave the CPU in, and this project has no separate banked-
        // register/privilege-mode behavior that depends on these bits either way.
        int sr = HleBootLoader.INITIAL_SR;

        assertEquals(1 << 30, sr & (1 << 30), "MD (bit 30) must still be set");
        assertEquals(1 << 29, sr & (1 << 29), "RB (bit 29) must still be set");
    }
}
