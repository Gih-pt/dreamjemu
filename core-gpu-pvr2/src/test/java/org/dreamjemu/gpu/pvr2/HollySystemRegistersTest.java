package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HollySystemRegistersTest {

    private static final long SB_ISTNRM_OFFSET = 0x100L;
    private static final int VBLANK_BEGIN_BIT = 1 << 3;

    @Test
    void istnrmAlwaysReportsVblankBeginPendingOnRead() {
        // Confirms the real-world case this class exists for: a real Sonic Adventure dump's
        // "while ((SB_ISTNRM & 0x8) == 0) {}" spin-wait (VBLANK_BEGIN, bit 3) needs every read to
        // already have that bit set for the loop to terminate promptly — see this class's
        // Javadoc for the full real-world context.
        HollySystemRegisters registers = new HollySystemRegisters();

        int value = registers.read32(SB_ISTNRM_OFFSET);

        assertTrue((value & VBLANK_BEGIN_BIT) != 0, "VBLANK_BEGIN must be set on the very first read");
        assertTrue((registers.read32(SB_ISTNRM_OFFSET) & VBLANK_BEGIN_BIT) != 0, "and every read after");
    }

    @Test
    void writingToIstnrmClearsAckedBitsUntilTheNextRead() {
        // Real hardware's documented write-1-to-clear acknowledgement convention. Confirms the
        // next read sets VBLANK_BEGIN again after an ack (since nothing tracks real frame timing
        // to know when a genuine next VBlank would occur — see this class's Javadoc).
        HollySystemRegisters registers = new HollySystemRegisters();
        registers.read32(SB_ISTNRM_OFFSET); // VBLANK_BEGIN now set

        registers.write32(SB_ISTNRM_OFFSET, VBLANK_BEGIN_BIT); // ack: write 1 to clear

        assertTrue((registers.read32(SB_ISTNRM_OFFSET) & VBLANK_BEGIN_BIT) != 0,
                "the next read must report VBLANK_BEGIN pending again, not stay cleared forever");
    }

    @Test
    void writingZeroToIstnrmLeavesItUnchanged() {
        // Write-1-to-clear means writing 0 to a bit must NOT set it — confirms this isn't
        // accidentally implemented as a plain overwrite.
        HollySystemRegisters registers = new HollySystemRegisters();
        registers.read32(SB_ISTNRM_OFFSET); // VBLANK_BEGIN now set

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
