package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HollySystemRegistersTest {

    private static final long SB_ISTNRM_OFFSET = 0x100L;
    private static final int VBLANK_BEGIN_BIT = HollySystemRegisters.VBLANK_BEGIN_BIT;

    @Test
    void readingIstnrmHasNoSideEffectAndDefaultsToNothingPending() {
        // Confirms the exact bug class this class was rewritten to fix: an earlier version made
        // VBLANK_BEGIN always read as pending, regardless of whether a real VBlank had ever
        // happened — see this class's Javadoc. A fresh instance, before setVblankBeginPending()
        // is ever called, must report nothing pending, and reading it must never change it.
        HollySystemRegisters registers = new HollySystemRegisters();

        int first = registers.read32(SB_ISTNRM_OFFSET);
        int second = registers.read32(SB_ISTNRM_OFFSET);

        assertEquals(0, first, "nothing pending until setVblankBeginPending() is genuinely called");
        assertEquals(first, second, "reading must not change the value");
        assertFalse(registers.hasPendingNormalInterrupt());
    }

    @Test
    void setVblankBeginPendingSetsTheBitForReal() {
        // The real, event-driven replacement for the old "always pending" placeholder — meant to
        // be called by app-cli's Main exactly when PvrRegisters.tick() reports a real VBlank.
        HollySystemRegisters registers = new HollySystemRegisters();

        registers.setVblankBeginPending();

        assertTrue((registers.read32(SB_ISTNRM_OFFSET) & VBLANK_BEGIN_BIT) != 0);
        assertTrue(registers.hasPendingNormalInterrupt());
    }

    @Test
    void writingToIstnrmClearsAckedBitsAndTheyStayClearedUntilGenuinelySetAgain() {
        // Real hardware's documented write-1-to-clear acknowledgement convention. Confirms an
        // acked bit stays clear afterward — unlike the old placeholder, which re-set it on the
        // very next read regardless of whether a real VBlank had actually happened again.
        HollySystemRegisters registers = new HollySystemRegisters();
        registers.setVblankBeginPending();

        registers.write32(SB_ISTNRM_OFFSET, VBLANK_BEGIN_BIT); // ack: write 1 to clear

        assertEquals(0, registers.read32(SB_ISTNRM_OFFSET), "must stay cleared, not re-set itself");
        assertFalse(registers.hasPendingNormalInterrupt());

        assertEquals(0, registers.read32(SB_ISTNRM_OFFSET), "and stay cleared across repeated reads");
    }

    @Test
    void writingZeroToIstnrmLeavesItUnchanged() {
        // Write-1-to-clear means writing 0 to a bit must NOT set it — confirms this isn't
        // accidentally implemented as a plain overwrite.
        HollySystemRegisters registers = new HollySystemRegisters();
        registers.setVblankBeginPending();

        registers.write32(SB_ISTNRM_OFFSET, 0);

        assertTrue((registers.read32(SB_ISTNRM_OFFSET) & VBLANK_BEGIN_BIT) != 0,
                "writing 0 must not clear VBLANK_BEGIN");
    }

    @Test
    void everyOtherRegisterFallsBackToUnmappedRegionBehavior() {
        // Confirms the "only SB_ISTNRM's VBLANK_BEGIN bit is modeled" boundary this class's
        // Javadoc describes: every other offset in the block (SB_C2DSTAT, SB_SFRES, SB_ISTEXT,
        // SB_ISTERR, ... dozens of others) must read as 0 across every access width, exactly
        // matching UnmappedRegion, until a real disc run confirms one of them is actually needed.
        HollySystemRegisters registers = new HollySystemRegisters();

        assertEquals((byte) 0, registers.read8(0x0000L)); // SB_C2DSTAT_addr
        assertEquals((short) 0, registers.read16(0x0090L)); // SB_SFRES_addr
        assertEquals(0, registers.read32(0x0104L)); // SB_ISTEXT_addr
        assertEquals(0L, registers.read64(0x0108L)); // SB_ISTERR_addr

        // Writes to any other offset are silently ignored — no exception, no state change.
        registers.write32(0x0090L, 0xDEADBEEF);
        assertEquals(0, registers.read32(0x0090L));
    }

    @Test
    void sizeAndNameMatchTheRealHollySystemControlBlock() {
        HollySystemRegisters registers = new HollySystemRegisters();

        assertEquals(0x200L, registers.size());
        assertEquals(HollySystemRegisters.SIZE, registers.size());
    }
}
