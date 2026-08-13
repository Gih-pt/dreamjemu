package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PvrRegistersTest {

    private static final long SPG_STATUS_OFFSET = 0x10CL;

    // Derived the same way PvrRegisters.STEPS_PER_LINE is (see its Javadoc): matches the real,
    // cited constants (SH-4's 200MHz clock, NTSC's 59.94Hz field rate and 262-line field length)
    // this class's tests need to know to drive a real tick() sequence deterministically.
    private static final long STEPS_PER_LINE = Math.round(200_000_000L / 59.94 / 262);
    private static final int LINES_PER_FIELD = 262;

    @Test
    void readingSpgStatusHasNoSideEffect() {
        // Confirms the exact bug class this class was rewritten to fix: an earlier version made
        // SPG_STATUS advance on every *read*, which is backwards (real video hardware runs
        // autonomously, not because software polled it) — see PvrRegisters' own Javadoc. Reading
        // it repeatedly, with no tick() calls in between, must never change its value.
        PvrRegisters registers = new PvrRegisters();

        int first = registers.read32(SPG_STATUS_OFFSET);
        int second = registers.read32(SPG_STATUS_OFFSET);
        int third = registers.read32(SPG_STATUS_OFFSET);

        assertEquals(0, first, "scanline starts at 0");
        assertEquals(first, second, "reading must not change the value");
        assertEquals(first, third, "reading must not change the value");
    }

    @Test
    void tickAdvancesScanlineOnlyAfterEnoughSteps() {
        // Confirms scanline genuinely reflects elapsed emulated time (steps), not read
        // frequency — the core fix this class exists for.
        PvrRegisters registers = new PvrRegisters();

        for (long i = 0; i < STEPS_PER_LINE - 1; i++) {
            assertFalse(registers.tick(), "must not report a VBlank before a full line's worth of steps");
        }
        assertEquals(0, registers.read32(SPG_STATUS_OFFSET), "scanline must still be 0 just before the line completes");

        registers.tick(); // the step that completes the first line

        assertEquals(1, registers.read32(SPG_STATUS_OFFSET), "scanline must advance to 1 after exactly one line's steps");
    }

    @Test
    void tickReportsVblankExactlyWhenTheFieldWraps() {
        // Confirms the real-world case this class exists for: after a full field's worth of
        // lines, tick() must report a VBlank (true) — this is what app-cli's Main uses to decide
        // when to set HollySystemRegisters' VBLANK_BEGIN and try to deliver a real interrupt.
        PvrRegisters registers = new PvrRegisters();
        long totalStepsInAField = STEPS_PER_LINE * LINES_PER_FIELD;

        boolean sawVblank = false;
        for (long i = 0; i < totalStepsInAField; i++) {
            if (registers.tick()) {
                assertFalse(sawVblank, "must report exactly one VBlank per field, not more");
                sawVblank = true;
                assertEquals(totalStepsInAField - 1, i, "must fire on the very last step of the field, not early or late");
            }
        }

        assertTrue(sawVblank, "must have reported a VBlank exactly once over a full field's worth of steps");
        assertEquals(0, registers.read32(SPG_STATUS_OFFSET), "scanline must have wrapped back to 0 after the field completes");
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
        assertEquals(0, registers.read32(SPG_STATUS_OFFSET), "a write to SPG_STATUS must not perturb the counter");
    }

    @Test
    void sizeAndNameMatchTheRealPvr2RegisterBlock() {
        PvrRegisters registers = new PvrRegisters();

        assertEquals(0x8000L, registers.size());
        assertEquals(PvrRegisters.SIZE, registers.size());
    }
}
