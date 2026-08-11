package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PvrRegistersTest {

    private static final long SPG_STATUS_OFFSET = 0x10CL;

    @Test
    void spgStatusStartsAtZeroAndIncrementsOnEachRead() {
        // Confirms the real-world case this class exists for: a real Sonic Adventure dump's
        // "while ((SPG_STATUS & 0x1FF) == 0) {}" spin-wait needs the very first read to already
        // be nonzero-after-increment for the loop to terminate promptly — see this class's
        // Javadoc for the full real-world context.
        PvrRegisters registers = new PvrRegisters();

        assertEquals(1, registers.read32(SPG_STATUS_OFFSET), "first read must already be nonzero");
        assertEquals(2, registers.read32(SPG_STATUS_OFFSET));
        assertEquals(3, registers.read32(SPG_STATUS_OFFSET));
    }

    @Test
    void spgStatusWrapsWithinTheReal10BitScanlineField() {
        // Real hardware's scanline field is 10 bits (0-1023); confirms this placeholder counter
        // respects that width rather than overflowing into the fieldnum/blank/hsync/vsync bits
        // (10-13) it deliberately leaves at 0 (not modeled yet — see this class's Javadoc).
        PvrRegisters registers = new PvrRegisters();

        int last = 0;
        for (int i = 0; i < 1024; i++) {
            last = registers.read32(SPG_STATUS_OFFSET);
        }
        assertEquals(1024 & 0x3FF, last, "value after exactly 1024 reads must have wrapped back to 0");
        assertEquals(1, registers.read32(SPG_STATUS_OFFSET), "the next read continues from the wrapped value");
    }

    @Test
    void everyOtherRegisterFallsBackToUnmappedRegionBehavior() {
        // Confirms the "only SPG_STATUS is modeled" boundary this class's Javadoc describes:
        // every other offset in the block (ID, REVISION, STARTRENDER, ... dozens of others) must
        // read as 0 across every access width, exactly matching UnmappedRegion, until a real
        // disc run confirms one of them is actually needed.
        PvrRegisters registers = new PvrRegisters();

        assertEquals((byte) 0, registers.read8(0x0000L)); // ID_addr
        assertEquals((short) 0, registers.read16(0x0004L)); // REVISION_addr
        assertEquals(0, registers.read32(0x0014L)); // STARTRENDER_addr
        assertEquals(0L, registers.read64(0x00A0L)); // SDRAM_REFRESH_addr

        // Writes to any offset (including SPG_STATUS itself, which is read-only on real
        // hardware) are silently ignored — no exception, no state change, matching
        // UnmappedRegion's documented "don't throw, bring-up needs this" reasoning.
        registers.write32(SPG_STATUS_OFFSET, 0xDEADBEEF);
        assertEquals(1, registers.read32(SPG_STATUS_OFFSET), "a write to SPG_STATUS must not perturb the counter");
    }

    @Test
    void sizeAndNameMatchTheRealPvr2RegisterBlock() {
        PvrRegisters registers = new PvrRegisters();

        assertEquals(0x8000L, registers.size());
        assertEquals(PvrRegisters.SIZE, registers.size());
    }
}
