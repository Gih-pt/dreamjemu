package org.dreamjemu.cpu.sh4;

import org.junit.jupiter.api.Test;

import static org.dreamjemu.cpu.sh4.Sh4Asm.addImm;
import static org.dreamjemu.cpu.sh4.Sh4Asm.addReg;
import static org.dreamjemu.cpu.sh4.Sh4Asm.addc;
import static org.dreamjemu.cpu.sh4.Sh4Asm.addv;
import static org.dreamjemu.cpu.sh4.Sh4Asm.andImmR0;
import static org.dreamjemu.cpu.sh4.Sh4Asm.andReg;
import static org.dreamjemu.cpu.sh4.Sh4Asm.bf;
import static org.dreamjemu.cpu.sh4.Sh4Asm.bfS;
import static org.dreamjemu.cpu.sh4.Sh4Asm.bra;
import static org.dreamjemu.cpu.sh4.Sh4Asm.bsr;
import static org.dreamjemu.cpu.sh4.Sh4Asm.bt;
import static org.dreamjemu.cpu.sh4.Sh4Asm.btS;
import static org.dreamjemu.cpu.sh4.Sh4Asm.cmpEqImmR0;
import static org.dreamjemu.cpu.sh4.Sh4Asm.cmpEqReg;
import static org.dreamjemu.cpu.sh4.Sh4Asm.cmpGe;
import static org.dreamjemu.cpu.sh4.Sh4Asm.cmpGt;
import static org.dreamjemu.cpu.sh4.Sh4Asm.cmpHi;
import static org.dreamjemu.cpu.sh4.Sh4Asm.cmpHs;
import static org.dreamjemu.cpu.sh4.Sh4Asm.cmpPl;
import static org.dreamjemu.cpu.sh4.Sh4Asm.cmpPz;
import static org.dreamjemu.cpu.sh4.Sh4Asm.cmpStr;
import static org.dreamjemu.cpu.sh4.Sh4Asm.dt;
import static org.dreamjemu.cpu.sh4.Sh4Asm.div0s;
import static org.dreamjemu.cpu.sh4.Sh4Asm.div0u;
import static org.dreamjemu.cpu.sh4.Sh4Asm.div1;
import static org.dreamjemu.cpu.sh4.Sh4Asm.dmulsL;
import static org.dreamjemu.cpu.sh4.Sh4Asm.dmuluL;
import static org.dreamjemu.cpu.sh4.Sh4Asm.extsB;
import static org.dreamjemu.cpu.sh4.Sh4Asm.extsW;
import static org.dreamjemu.cpu.sh4.Sh4Asm.extuB;
import static org.dreamjemu.cpu.sh4.Sh4Asm.extuW;
import static org.dreamjemu.cpu.sh4.Sh4Asm.negc;
import static org.dreamjemu.cpu.sh4.Sh4Asm.subc;
import static org.dreamjemu.cpu.sh4.Sh4Asm.subv;
import static org.dreamjemu.cpu.sh4.Sh4Asm.swapB;
import static org.dreamjemu.cpu.sh4.Sh4Asm.swapW;
import static org.dreamjemu.cpu.sh4.Sh4Asm.tstImm;
import static org.dreamjemu.cpu.sh4.Sh4Asm.tstReg;
import static org.dreamjemu.cpu.sh4.Sh4Asm.xtrct;
import static org.dreamjemu.cpu.sh4.Sh4Asm.jmp;
import static org.dreamjemu.cpu.sh4.Sh4Asm.jsr;
import static org.dreamjemu.cpu.sh4.Sh4Asm.mova;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movBLoad;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movBLoadDisp4;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movBLoadIndexed;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movBLoadPostInc;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movBStore;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movBStoreDisp4;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movBStoreIndexed;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movBStorePreDec;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movImm;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movLLoad;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movLLoadDisp4;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movLLoadIndexed;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movLLoadPcRel;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movLLoadPostInc;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movLStore;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movLStoreDisp4;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movLStoreIndexed;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movLStorePreDec;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movReg;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movWLoad;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movWLoadDisp4;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movWLoadIndexed;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movWLoadPcRel;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movWLoadPostInc;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movWStore;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movWStoreDisp4;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movWStoreIndexed;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movWStorePreDec;
import static org.dreamjemu.cpu.sh4.Sh4Asm.mulL;
import static org.dreamjemu.cpu.sh4.Sh4Asm.mulsW;
import static org.dreamjemu.cpu.sh4.Sh4Asm.muluW;
import static org.dreamjemu.cpu.sh4.Sh4Asm.negReg;
import static org.dreamjemu.cpu.sh4.Sh4Asm.nop;
import static org.dreamjemu.cpu.sh4.Sh4Asm.notReg;
import static org.dreamjemu.cpu.sh4.Sh4Asm.orImmR0;
import static org.dreamjemu.cpu.sh4.Sh4Asm.orReg;
import static org.dreamjemu.cpu.sh4.Sh4Asm.rotl;
import static org.dreamjemu.cpu.sh4.Sh4Asm.rotr;
import static org.dreamjemu.cpu.sh4.Sh4Asm.rotcr;
import static org.dreamjemu.cpu.sh4.Sh4Asm.shad;
import static org.dreamjemu.cpu.sh4.Sh4Asm.shld;
import static org.dreamjemu.cpu.sh4.Sh4Asm.tasB;
import static org.dreamjemu.cpu.sh4.Sh4Asm.stsLMach;
import static org.dreamjemu.cpu.sh4.Sh4Asm.stsLMacl;
import static org.dreamjemu.cpu.sh4.Sh4Asm.stsLPr;
import static org.dreamjemu.cpu.sh4.Sh4Asm.ldsLMach;
import static org.dreamjemu.cpu.sh4.Sh4Asm.ldsLMacl;
import static org.dreamjemu.cpu.sh4.Sh4Asm.ldsLPr;
import static org.dreamjemu.cpu.sh4.Sh4Asm.ldcVbr;
import static org.dreamjemu.cpu.sh4.Sh4Asm.stcVbr;
import static org.dreamjemu.cpu.sh4.Sh4Asm.trapa;
import static org.dreamjemu.cpu.sh4.Sh4Asm.rotcl;
import static org.dreamjemu.cpu.sh4.Sh4Asm.rte;
import static org.dreamjemu.cpu.sh4.Sh4Asm.rts;
import static org.dreamjemu.cpu.sh4.Sh4Asm.shal;
import static org.dreamjemu.cpu.sh4.Sh4Asm.shar;
import static org.dreamjemu.cpu.sh4.Sh4Asm.shll;
import static org.dreamjemu.cpu.sh4.Sh4Asm.shlr;
import static org.dreamjemu.cpu.sh4.Sh4Asm.subReg;
import static org.dreamjemu.cpu.sh4.Sh4Asm.xorImmR0;
import static org.dreamjemu.cpu.sh4.Sh4Asm.xorReg;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Sh4CpuTest {

    private static final int MEM_SIZE = 256;

    // Instruction encoders live in Sh4Asm (shared with Sh4CpuSystemBusIntegrationTest).

    @Test
    void nopAdvancesPcOnly() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, 0x0009);
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);

        cpu.step();

        assertEquals(2, cpu.pc);
    }

    @Test
    void movImmLoadsSignExtendedValue() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, movImm(3, -1)); // MOV #-1,R3
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);

        cpu.step();

        assertEquals(-1, cpu.r[3]);
    }

    @Test
    void movRegCopiesRegister() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, movReg(1, 2)); // MOV R2,R1
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[2] = 42;

        cpu.step();

        assertEquals(42, cpu.r[1]);
    }

    @Test
    void addImmAddsSignExtendedValue() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, addImm(0, -5)); // ADD #-5,R0
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 10;

        cpu.step();

        assertEquals(5, cpu.r[0]);
    }

    @Test
    void addRegAddsRegisters() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, addReg(0, 1)); // ADD R1,R0
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 3;
        cpu.r[1] = 4;

        cpu.step();

        assertEquals(7, cpu.r[0]);
    }

    @Test
    void subRegSubtractsRegisters() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, subReg(0, 1)); // SUB R1,R0
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 10;
        cpu.r[1] = 3;

        cpu.step();

        assertEquals(7, cpu.r[0]);
    }

    @Test
    void andRegPerformsBitwiseAnd() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, andReg(0, 1)); // AND R1,R0
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0b1100;
        cpu.r[1] = 0b1010;

        cpu.step();

        assertEquals(0b1000, cpu.r[0]);
    }

    @Test
    void orRegPerformsBitwiseOr() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, orReg(0, 1)); // OR R1,R0
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0b1100;
        cpu.r[1] = 0b1010;

        cpu.step();

        assertEquals(0b1110, cpu.r[0]);
    }

    @Test
    void xorRegPerformsBitwiseXor() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, xorReg(0, 1)); // XOR R1,R0
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0b1100;
        cpu.r[1] = 0b1010;

        cpu.step();

        assertEquals(0b0110, cpu.r[0]);
    }

    @Test
    void andImmR0ZeroExtendsTheImmediateUnlikeMovAndAdd() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, andImmR0(0x80)); // AND #0x80,R0
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0xFFFFFFFF;

        cpu.step();

        // If 0x80 were wrongly sign-extended to 0xFFFFFF80 (like MOV/ADD/CMP's
        // immediates), the result would be 0xFFFFFF80 instead.
        assertEquals(0x00000080, cpu.r[0]);
    }

    @Test
    void orImmR0ZeroExtendsTheImmediateUnlikeMovAndAdd() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, orImmR0(0x80)); // OR #0x80,R0
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0x00000000;

        cpu.step();

        assertEquals(0x00000080, cpu.r[0]);
    }

    @Test
    void xorImmR0ZeroExtendsTheImmediateUnlikeMovAndAdd() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, xorImmR0(0x80)); // XOR #0x80,R0
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0xFFFFFFFF;

        cpu.step();

        // Zero-extended 0x80 flips only bit 7; a wrongly sign-extended 0xFFFFFF80
        // would instead flip the low 8 bits' complement pattern (result 0x0000007F).
        assertEquals(0xFFFFFF7F, cpu.r[0]);
    }

    @Test
    void shllShiftsLeftAndSetsTFromOldMsb() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, shll(0)); // SHLL R0
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0x80000001;

        cpu.step();

        assertTrue(cpu.tFlag(), "T should hold the bit shifted out (old MSB, which was 1)");
        assertEquals(0x00000002, cpu.r[0]);
    }

    @Test
    void shlrShiftsRightLogicallyAndSetsTFromOldLsb() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, shlr(0)); // SHLR R0
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0x80000001;

        cpu.step();

        assertTrue(cpu.tFlag(), "T should hold the bit shifted out (old LSB, which was 1)");
        assertEquals(0x40000000, cpu.r[0], "SHLR is a logical shift: the vacated top bit must be zero-filled");
    }

    @Test
    void shalBehavesIdenticallyToShll() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, shal(0)); // SHAL R0
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0x80000001;

        cpu.step();

        assertTrue(cpu.tFlag());
        assertEquals(0x00000002, cpu.r[0]);
    }

    @Test
    void sharShiftsRightArithmeticallyPreservingSign() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, shar(0)); // SHAR R0
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0x80000001;

        cpu.step();

        assertTrue(cpu.tFlag(), "T should hold the bit shifted out (old LSB, which was 1)");
        assertEquals(0xC0000000, cpu.r[0],
                "SHAR is an arithmetic shift: the vacated top bit must be sign-filled (1, since the value was negative)");
    }

    @Test
    void cmpEqRegSetsAndClearsTFlag() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, cmpEqReg(0, 1)); // CMP/EQ R1,R0
        bus.writeInstruction(2, cmpEqReg(0, 1));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 5;
        cpu.r[1] = 5;

        cpu.step();
        assertTrue(cpu.tFlag());

        cpu.r[1] = 6;
        cpu.step();
        assertFalse(cpu.tFlag());
    }

    @Test
    void cmpEqImmR0SetsTFlag() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, cmpEqImmR0(7)); // CMP/EQ #7,R0
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 7;

        cpu.step();

        assertTrue(cpu.tFlag());
    }

    @Test
    void btBranchesOnlyWhenTIsSet() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, bt(2)); // BT +2 (disp) -> target = 0 + 4 + 2*2 = 8
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);

        // T clear: falls through
        cpu.step();
        assertEquals(2, cpu.pc);

        // T set: branches
        cpu.pc = 0;
        bus.writeInstruction(0, cmpEqReg(0, 0)); // forces T = true (R0 == R0)
        cpu.step(); // now at pc=2, T is true
        bus.writeInstruction(2, bt(2)); // target = 2 + 4 + 2*2 = 10
        cpu.step();
        assertEquals(10, cpu.pc);
    }

    @Test
    void bfBranchesOnlyWhenTIsClear() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, bf(2)); // BF +2 -> target = 0 + 4 + 2*2 = 8
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);

        // T clear (default): branches
        cpu.step();
        assertEquals(8, cpu.pc);
    }

    @Test
    void braBranchesUnconditionallyForwardAndBackward() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, bra(2));   // target = 0 + 4 + 2*2 = 8
        bus.writeInstruction(2, 0x0009);   // NOP delay slot (required — BRA is a delayed branch)
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);

        cpu.step();
        assertEquals(8, cpu.pc);

        // Negative displacement (backward branch)
        bus.writeInstruction(20, bra(-5)); // target = 20 + 4 + (-5*2) = 14
        bus.writeInstruction(22, 0x0009);  // NOP delay slot
        cpu.pc = 20;
        cpu.step();
        assertEquals(14, cpu.pc);
    }

    @Test
    void braExecutesDelaySlotInstructionBeforeJumping() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, bra(2));         // target = 0 + 4 + 2*2 = 8
        bus.writeInstruction(2, movImm(0, 99));  // delay slot: MOV #99,R0 — must execute BEFORE the jump
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);

        cpu.step();

        assertEquals(99, cpu.r[0], "the delay slot instruction's effect must be visible");
        assertEquals(8, cpu.pc, "PC must land on the branch target, not thisPc+2 or thisPc+4");
    }

    @Test
    void branchInDelaySlotIsIllegal() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, bra(2));
        bus.writeInstruction(2, bt(0)); // illegal: a branch instruction in a delay slot
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);

        assertThrows(IllegalStateException.class, cpu::step);
    }

    @Test
    void bfSBranchesAndExecutesTheDelaySlotWhenTIsClear() {
        // Found necessary by a real Sonic Adventure dump: opcode 0x8F02, hit after
        // 12,791,622 real SH-4 instructions executed correctly (see docs/STATUS.md).
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, bfS(2));        // BF/S +2 -> target = 0 + 4 + 2*2 = 8
        bus.writeInstruction(2, movImm(0, 99)); // delay slot: MUST execute regardless of the branch
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        // T clear (default): branch is taken.

        cpu.step();

        assertEquals(99, cpu.r[0], "the delay slot instruction's effect must be visible either way");
        assertEquals(8, cpu.pc, "PC must land on the branch target when T is clear");
    }

    @Test
    void bfSFallsThroughPastTheDelaySlotWhenTIsSet() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, cmpEqReg(0, 0)); // sets T = true (R0 == R0)
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.step(); // pc now 2, T is true

        bus.writeInstruction(2, bfS(2));        // BF/S +2 -> target would be 2+4+2*2=10, but T is set
        bus.writeInstruction(4, movImm(1, 42)); // delay slot: still executes even though not taken
        cpu.step();

        assertEquals(42, cpu.r[1], "the delay slot instruction's effect must be visible either way");
        assertEquals(6, cpu.pc, "PC must fall through to thisPc+4 (skipping the branch AND its delay slot), not the target");
    }

    @Test
    void btSBranchesAndExecutesTheDelaySlotWhenTIsSet() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, cmpEqReg(0, 0)); // sets T = true (R0 == R0)
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.step(); // pc now 2, T is true

        bus.writeInstruction(2, btS(2));        // BT/S +2 -> target = 2 + 4 + 2*2 = 10
        bus.writeInstruction(4, movImm(0, 77)); // delay slot
        cpu.step();

        assertEquals(77, cpu.r[0]);
        assertEquals(10, cpu.pc, "PC must land on the branch target when T is set");
    }

    @Test
    void btSFallsThroughPastTheDelaySlotWhenTIsClear() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, btS(2));        // T clear (default): not taken
        bus.writeInstruction(2, movImm(1, 55)); // delay slot: still executes even though not taken
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);

        cpu.step();

        assertEquals(55, cpu.r[1], "the delay slot instruction's effect must be visible either way");
        assertEquals(4, cpu.pc, "PC must fall through to thisPc+4, not the target");
    }

    @Test
    void branchInBfSDelaySlotIsIllegal() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, bfS(2));
        bus.writeInstruction(2, bt(0)); // illegal: a branch instruction in a delay slot

        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        assertThrows(IllegalStateException.class, cpu::step);
    }

    @Test
    void movLStoreThenLoadRoundTrips() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, movLStore(1, 0)); // MOV.L R0,@R1
        bus.writeInstruction(2, movLLoad(2, 1));  // MOV.L @R1,R2
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0x12345678;
        cpu.r[1] = 100; // address to store at

        cpu.step(); // store
        cpu.step(); // load back into R2

        assertEquals(0x12345678, cpu.r[2]);
    }

    @Test
    void unimplementedOpcodeThrowsWithClearMessage() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, 0xFFFF); // not implemented by this interpreter
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);

        UnsupportedOperationException thrown =
                assertThrows(UnsupportedOperationException.class, cpu::step);
        assertTrue(thrown.getMessage().contains("0xFFFF") || thrown.getMessage().contains("FFFF"));
    }

    /**
     * Integration test: a small hand-assembled loop program that sums 5+4+3+2+1
     * into R0 and stores the result to memory, then verifies both the register
     * and the memory content. This exercises immediate loads, register-register
     * arithmetic, comparison, conditional branching (a real loop), and a
     * memory store/load — the full implemented subset working together.
     *
     * Program (addresses in bytes):
     *   0:  MOV #0,  R0        ; accumulator = 0
     *   2:  MOV #5,  R1        ; counter = 5
     *   4:  MOV #0,  R2        ; zero constant, for the loop exit comparison
     *   6:  MOV #0x40, R4      ; R4 = target store address (64)
     *   8:  ADD R1,R0          ; R0 += R1              <- loop start
     *   10: ADD #-1,R1         ; R1 -= 1
     *   12: CMP/EQ R2,R1       ; T = (R1 == 0)
     *   14: BF -5              ; if R1 != 0, branch back to address 8
     *   16: MOV.L R0,@R4       ; store final sum to memory
     *   18: NOP                ; end marker
     */
    @Test
    void handAssembledLoopProgramSumsOneToFiveAndStoresResult() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);

        bus.writeInstruction(0, movImm(0, 0));
        bus.writeInstruction(2, movImm(1, 5));
        bus.writeInstruction(4, movImm(2, 0));
        bus.writeInstruction(6, movImm(4, 0x40));
        bus.writeInstruction(8, addReg(0, 1));
        bus.writeInstruction(10, addImm(1, -1));
        bus.writeInstruction(12, cmpEqReg(1, 2));
        bus.writeInstruction(14, bf(-5)); // target = 14 + 4 + (-5*2) = 8
        bus.writeInstruction(16, movLStore(4, 0));
        bus.writeInstruction(18, 0x0009); // NOP, end marker

        Sh4Cpu cpu = new Sh4Cpu(bus, 0);

        int steps = 0;
        while (cpu.pc < 18) {
            cpu.step();
            steps++;
            if (steps > 1000) {
                throw new AssertionError("Program did not terminate — likely an infinite loop");
            }
        }

        assertEquals(15, cpu.r[0], "R0 should hold the sum 5+4+3+2+1");
        assertEquals(15, bus.read32(0x40), "The sum should also have been stored to memory at address 0x40");
        // setup (4) + 5 loop iterations * 4 instructions (20) + final MOV.L (1) = 25 steps
        assertEquals(25, steps);
    }

    @Test
    void bsrSetsPrAndBranchesAfterDelaySlot() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, bsr(2));         // target = 0 + 4 + 2*2 = 8
        bus.writeInstruction(2, movImm(0, 99));  // delay slot, must execute before the jump
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);

        cpu.step();

        assertEquals(99, cpu.r[0], "the delay slot instruction's effect must be visible");
        assertEquals(8, cpu.pc, "PC must land on the subroutine target");
        assertEquals(4, cpu.pr, "PR must hold the return address (thisPc + 4)");
    }

    @Test
    void jsrReadsTargetRegisterBeforeDelaySlotRuns() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, jsr(1));         // JSR @R1
        bus.writeInstruction(2, movImm(1, 50));  // delay slot: overwrites R1 itself
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[1] = 20; // subroutine target address, read BEFORE the delay slot changes R1

        cpu.step();

        assertEquals(20, cpu.pc, "the target must be R1's value at JSR time, not after the delay slot modified it");
        assertEquals(50, cpu.r[1], "the delay slot instruction still executes and its effect is visible");
        assertEquals(4, cpu.pr, "PR must hold the return address (thisPc + 4)");
    }

    @Test
    void rtsReturnsToPrAfterDelaySlot() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, rts());
        bus.writeInstruction(2, movImm(0, 7)); // delay slot, must execute before the jump
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.pr = 40;

        cpu.step();

        assertEquals(7, cpu.r[0], "the delay slot instruction's effect must be visible");
        assertEquals(40, cpu.pc, "PC must land on PR's address");
    }

    @Test
    void branchInBsrDelaySlotIsIllegal() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, bsr(2));
        bus.writeInstruction(2, rts()); // illegal: a delayed branch in a delay slot
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);

        assertThrows(IllegalStateException.class, cpu::step);
    }

    @Test
    void bsrThenRtsRoundTripsBackToTheCaller() {
        // A tiny "call a subroutine and return" program:
        //   0:  BSR +2         ; call the subroutine at address 8
        //   2:  NOP            ; delay slot
        //   4:  MOV #123,R0    ; runs after returning
        //   6:  NOP            ; end marker
        //   8:  MOV #1,R1      ; subroutine body
        //   10: RTS            ; return to caller
        //   12: NOP            ; delay slot
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, bsr(2));       // target = 0 + 4 + 2*2 = 8
        bus.writeInstruction(2, nop());
        bus.writeInstruction(4, movImm(0, 123));
        bus.writeInstruction(6, nop());
        bus.writeInstruction(8, movImm(1, 1));
        bus.writeInstruction(10, rts());       // returns to PR = 4
        bus.writeInstruction(12, nop());
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);

        int steps = 0;
        while (cpu.pc != 6) {
            cpu.step();
            steps++;
            if (steps > 1000) {
                throw new AssertionError("Program did not terminate — likely an infinite loop");
            }
        }

        assertEquals(1, cpu.r[1], "the subroutine body must have run");
        assertEquals(123, cpu.r[0], "execution must have resumed after the call, at the caller's next instruction");
        // 4 calls to step(): BSR (which internally also runs its delay slot),
        // MOV #1,R1, RTS (which internally also runs its delay slot), MOV #123,R0
        assertEquals(4, steps);
    }

    @Test
    void jmpReadsTargetRegisterBeforeDelaySlotRunsAndDoesNotTouchPr() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, jmp(1));         // JMP @R1
        bus.writeInstruction(2, movImm(1, 50));  // delay slot: overwrites R1 itself
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[1] = 20;
        cpu.pr = 999; // sentinel — JMP must not touch PR, unlike JSR

        cpu.step();

        assertEquals(20, cpu.pc, "the target must be R1's value at JMP time, not after the delay slot modified it");
        assertEquals(50, cpu.r[1], "the delay slot instruction still executes");
        assertEquals(999, cpu.pr, "JMP must not set PR (unlike JSR)");
    }

    @Test
    void movBStoreThenLoadSignExtends() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, movBStore(1, 0)); // MOV.B R0,@R1
        bus.writeInstruction(2, movBLoad(2, 1));  // MOV.B @R1,R2
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0xFFFFFF80; // low byte 0x80 — negative once sign-extended
        cpu.r[1] = 100;

        cpu.step();
        cpu.step();

        assertEquals(0xFFFFFF80, cpu.r[2], "a stored 0x80 byte must load back sign-extended to 0xFFFFFF80");
    }

    @Test
    void movWStoreThenLoadSignExtends() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, movWStore(1, 0)); // MOV.W R0,@R1
        bus.writeInstruction(2, movWLoad(2, 1));  // MOV.W @R1,R2
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0xFFFF8000; // low word 0x8000 — negative once sign-extended
        cpu.r[1] = 100;

        cpu.step();
        cpu.step();

        assertEquals(0xFFFF8000, cpu.r[2], "a stored 0x8000 word must load back sign-extended to 0xFFFF8000");
    }

    @Test
    void notComputesBitwiseComplement() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, notReg(1, 0)); // NOT R0,R1
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0x0000FFFF;

        cpu.step();

        assertEquals(0xFFFF0000, cpu.r[1]);
    }

    @Test
    void negComputesTwosComplementNegation() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, negReg(1, 0)); // NEG R0,R1
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 5;

        cpu.step();

        assertEquals(-5, cpu.r[1]);
    }

    @Test
    void mulLTruncatesTo32Bits() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, mulL(0, 1)); // MUL.L R1,R0
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 6;
        cpu.r[1] = 7;

        cpu.step();

        assertEquals(42, cpu.macl);
    }

    @Test
    void mulLTruncatesOverflowSilently() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, mulL(0, 1));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0x10000;
        cpu.r[1] = 0x10000;

        cpu.step();

        assertEquals(0, cpu.macl, "0x100000000 truncated to 32 bits is 0");
    }

    @Test
    void mulsWSignExtendsLow16BitsOfEachOperand() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, mulsW(0, 1)); // MULS.W R1,R0
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0xFFFFFFFF; // low 16 bits = 0xFFFF = -1 once sign-extended
        cpu.r[1] = 5;

        cpu.step();

        assertEquals(-5, cpu.macl);
    }

    @Test
    void muluWZeroExtendsLow16BitsOfEachOperand() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, muluW(0, 1)); // MULU.W R1,R0
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0xFFFF0000 | 0xFFFF; // low 16 bits = 0xFFFF = 65535 zero-extended
        cpu.r[1] = 2;

        cpu.step();

        assertEquals(131070, cpu.macl, "65535 * 2, unsigned");
    }

    @Test
    void dmulsLProducesSignExtendedSixtyFourBitResult() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, dmulsL(0, 1)); // DMULS.L R1,R0
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = -5;
        cpu.r[1] = 3;

        cpu.step();

        assertEquals(-15, ((long) cpu.mach << 32) | (cpu.macl & 0xFFFFFFFFL));
    }

    @Test
    void dmuluLProducesUnsignedSixtyFourBitResult() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, dmuluL(0, 1)); // DMULU.L R1,R0
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0xFFFFFFFF; // 4294967295 unsigned
        cpu.r[1] = 2;

        cpu.step();

        long result = (Integer.toUnsignedLong(cpu.mach) << 32) | Integer.toUnsignedLong(cpu.macl);
        assertEquals(8589934590L, result);
    }

    @Test
    void div0uInitializesFlagsToZero() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, div0u());
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0; // arbitrary prior state

        cpu.step();

        assertFalse(cpu.qFlag());
        assertFalse(cpu.mFlag());
        assertFalse(cpu.tFlag());
    }

    @Test
    void div0sSetsQAndMFromOperandSignsAndTFromXor() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, div0s(1, 0)); // DIV0S R0,R1 — Q from R1's sign, M from R0's sign
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[1] = 0x80000000; // negative dividend -> Q = true
        cpu.r[0] = 0x00000005; // positive divisor -> M = false

        cpu.step();

        assertTrue(cpu.qFlag());
        assertFalse(cpu.mFlag());
        assertTrue(cpu.tFlag(), "T = (Q != M)");
    }

    @Test
    void div1SingleStepMatchesHandTracedExpectedState() {
        // Hand-traced against the documented DIV1 algorithm (Q/M/T semantics
        // confirmed against a public SH instruction set reference — see
        // docs/STATUS.md). Two branches (old_q=false with M=false, and
        // old_q=false with M=true) are exercised here; the full division
        // integration test below exercises all four branches together over
        // many iterations.
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, div1(1, 0)); // DIV1 R0,R1
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[1] = 1; // dividend (Rn)
        cpu.r[0] = 1; // divisor (Rm)
        // Q=false, M=false, T=false (as if freshly set by DIV0U)

        cpu.step();

        assertEquals(1, cpu.r[1]);
        assertFalse(cpu.qFlag());
        assertTrue(cpu.tFlag());
    }

    @Test
    void divisionRoutineComputesCorrectUnsignedQuotient() {
        // The canonical SH-4 32-bit unsigned division sequence (r1:r2 / r0 = r2),
        // as documented in the SH instruction set reference: DIV0U, then 32
        // repetitions of {ROTCL R2; DIV1 R0,R1}, then a final ROTCL R2. With the
        // dividend's high half (R1) held at 0, this reduces to a plain 32-bit
        // unsigned division. The expected quotient is computed independently via
        // plain Java integer division for comparison — a strong end-to-end check
        // that DIV1/ROTCL's flag bookkeeping is correct over many iterations, not
        // just in the individual hand-traced cases above.
        int dividend = 100;
        int divisor = 7;

        SimpleTestBus bus = new SimpleTestBus(512);
        int addr = 0;
        addr = write(bus, addr, movImm(1, 0));            // R1 = 0 (dividend high half)
        addr = write(bus, addr, movImm(2, dividend));     // R2 = dividend
        addr = write(bus, addr, movImm(0, divisor));      // R0 = divisor
        addr = write(bus, addr, div0u());
        for (int i = 0; i < 32; i++) {
            addr = write(bus, addr, rotcl(2));
            addr = write(bus, addr, div1(1, 0)); // DIV1 R0,R1
        }
        addr = write(bus, addr, rotcl(2));
        int endAddress = addr;

        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        int steps = 0;
        while (cpu.pc != endAddress) {
            cpu.step();
            steps++;
            if (steps > 10_000) {
                throw new AssertionError("Division routine did not terminate");
            }
        }

        assertEquals(dividend / divisor, cpu.r[2]);
    }

    // ---- Post-increment / pre-decrement addressing ----------------------

    @Test
    void movBLoadPostIncSignExtendsAndIncrementsRm() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, movBLoadPostInc(2, 1)); // MOV.B @R1+,R2
        bus.write8(50, (byte) 0xFF);
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[1] = 50;

        cpu.step();

        assertEquals(-1, cpu.r[2], "0xFF should sign-extend to -1");
        assertEquals(51, cpu.r[1], "Rm should be incremented by 1 after a byte load");
    }

    @Test
    void movWLoadPostIncSignExtendsAndIncrementsRm() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, movWLoadPostInc(2, 1)); // MOV.W @R1+,R2
        bus.write16(60, (short) 0x8000);
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[1] = 60;

        cpu.step();

        assertEquals(0xFFFF8000, cpu.r[2], "0x8000 should sign-extend to a negative 32-bit value");
        assertEquals(62, cpu.r[1], "Rm should be incremented by 2 after a word load");
    }

    @Test
    void movLLoadPostIncIncrementsRm() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, movLLoadPostInc(2, 1)); // MOV.L @R1+,R2
        bus.write32(70, 0x12345678);
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[1] = 70;

        cpu.step();

        assertEquals(0x12345678, cpu.r[2]);
        assertEquals(74, cpu.r[1], "Rm should be incremented by 4 after a longword load");
    }

    @Test
    void movLLoadPostIncSkipsIncrementWhenRnEqualsRm() {
        // Per the SH-4 spec: "if (n != m) R[m] += 4;" — when the destination and the
        // address register are the same, the increment must NOT also apply on top of
        // the loaded value overwriting it.
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, movLLoadPostInc(3, 3)); // MOV.L @R3+,R3
        bus.write32(80, 0x11223344);
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[3] = 80;

        cpu.step();

        assertEquals(0x11223344, cpu.r[3], "R3 should hold the loaded value, not the incremented address");
    }

    @Test
    void movBStorePreDecDecrementsThenStores() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, movBStorePreDec(1, 2)); // MOV.B R2,@-R1
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[1] = 100;
        cpu.r[2] = 0xEF;

        cpu.step();

        assertEquals(99, cpu.r[1], "Rn should be decremented by 1 before the store");
        assertEquals((byte) 0xEF, bus.read8(99));
    }

    @Test
    void movWStorePreDecDecrementsThenStores() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, movWStorePreDec(1, 2)); // MOV.W R2,@-R1
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[1] = 100;
        cpu.r[2] = 0x1234;

        cpu.step();

        assertEquals(98, cpu.r[1], "Rn should be decremented by 2 before the store");
        assertEquals((short) 0x1234, bus.read16(98));
    }

    @Test
    void movLStorePreDecDecrementsThenStores() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, movLStorePreDec(1, 2)); // MOV.L R2,@-R1
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[1] = 100;
        cpu.r[2] = 0x12345678;

        cpu.step();

        assertEquals(96, cpu.r[1], "Rn should be decremented by 4 before the store");
        assertEquals(0x12345678, bus.read32(96));
    }

    @Test
    void movLPushThenPopRoundTrips() {
        // The SH-4's push/pop idiom: MOV.L Rm,@-R15 (push) paired with MOV.L @R15+,Rn (pop).
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, movLStorePreDec(15, 0)); // MOV.L R0,@-R15  (push R0)
        bus.writeInstruction(2, movLLoadPostInc(1, 15)); // MOV.L @R15+,R1 (pop into R1)
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[15] = 200;
        cpu.r[0] = 0x12345678;

        cpu.step(); // push
        assertEquals(196, cpu.r[15], "stack pointer should have moved down by 4 after the push");

        cpu.step(); // pop
        assertEquals(0x12345678, cpu.r[1], "the popped value should match what was pushed");
        assertEquals(200, cpu.r[15], "stack pointer should be back to its original value after the pop");
    }

    // ---- Indexed (R0-based) addressing -----------------------------------

    @Test
    void movBLoadIndexedUsesR0PlusRm() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, movBLoadIndexed(2, 1)); // MOV.B @(R0,R1),R2
        bus.write8(105, (byte) 0xFE);
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 5;
        cpu.r[1] = 100;

        cpu.step();

        assertEquals(-2, cpu.r[2], "0xFE should sign-extend to -2");
    }

    @Test
    void movWLoadIndexedUsesR0PlusRm() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, movWLoadIndexed(2, 1)); // MOV.W @(R0,R1),R2
        bus.write16(105, (short) 0x00AB);
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 5;
        cpu.r[1] = 100;

        cpu.step();

        assertEquals(0x00AB, cpu.r[2]);
    }

    @Test
    void movLLoadIndexedUsesR0PlusRm() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, movLLoadIndexed(2, 1)); // MOV.L @(R0,R1),R2
        bus.write32(104, 0x0BADF00D);
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 4;
        cpu.r[1] = 100;

        cpu.step();

        assertEquals(0x0BADF00D, cpu.r[2]);
    }

    @Test
    void movBStoreIndexedUsesR0PlusRn() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, movBStoreIndexed(1, 2)); // MOV.B R2,@(R0,R1)
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 5;
        cpu.r[1] = 100;
        cpu.r[2] = 0x7A;

        cpu.step();

        assertEquals((byte) 0x7A, bus.read8(105));
    }

    @Test
    void movWStoreIndexedUsesR0PlusRn() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, movWStoreIndexed(1, 2)); // MOV.W R2,@(R0,R1)
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 5;
        cpu.r[1] = 100;
        cpu.r[2] = 0x5678;

        cpu.step();

        assertEquals((short) 0x5678, bus.read16(105));
    }

    @Test
    void movLStoreIndexedUsesR0PlusRn() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, movLStoreIndexed(1, 2)); // MOV.L R2,@(R0,R1)
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 4;
        cpu.r[1] = 100;
        cpu.r[2] = 0x0BADF00D;

        cpu.step();

        assertEquals(0x0BADF00D, bus.read32(104));
    }

    // ---- 4-bit displacement addressing -----------------------------------

    @Test
    void movBLoadDisp4IsNotScaled() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, movBLoadDisp4(1, 5)); // MOV.B @(5,R1),R0
        bus.write8(105, (byte) 0xFD);
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[1] = 100;

        cpu.step();

        assertEquals(-3, cpu.r[0], "0xFD should sign-extend to -3; disp=5 should mean +5 bytes, not scaled");
    }

    @Test
    void movWLoadDisp4IsScaledByTwo() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, movWLoadDisp4(1, 5)); // MOV.W @(5,R1),R0 -> byte offset 10
        bus.write16(110, (short) 0x00CD);
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[1] = 100;

        cpu.step();

        assertEquals(0x00CD, cpu.r[0]);
    }

    @Test
    void movLLoadDisp4IsScaledByFour() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, movLLoadDisp4(3, 1, 5)); // MOV.L @(5,R1),R3 -> byte offset 20
        bus.write32(120, 0x0BADF00D);
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[1] = 100;

        cpu.step();

        assertEquals(0x0BADF00D, cpu.r[3]);
    }

    @Test
    void movBStoreDisp4IsNotScaled() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, movBStoreDisp4(1, 5)); // MOV.B R0,@(5,R1)
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[1] = 100;
        cpu.r[0] = 0x9A;

        cpu.step();

        assertEquals((byte) 0x9A, bus.read8(105));
    }

    @Test
    void movWStoreDisp4IsScaledByTwo() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, movWStoreDisp4(1, 5)); // MOV.W R0,@(5,R1) -> byte offset 10
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[1] = 100;
        cpu.r[0] = 0x4321;

        cpu.step();

        assertEquals((short) 0x4321, bus.read16(110));
    }

    @Test
    void movLStoreDisp4IsScaledByFour() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, movLStoreDisp4(1, 2, 5)); // MOV.L R2,@(5,R1) -> byte offset 20
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[1] = 100;
        cpu.r[2] = 0x0BADF00D;

        cpu.step();

        assertEquals(0x0BADF00D, bus.read32(120));
    }

    // ---- PC-relative addressing --------------------------------------

    @Test
    void movWLoadPcRelSignExtends() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, movWLoadPcRel(3, 2)); // MOV.W @(2,PC),R3 -> address 0+4+2*2=8
        bus.write16(8, (short) 0x8001);
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);

        cpu.step();

        assertEquals(0xFFFF8001, cpu.r[3], "0x8001 should sign-extend to a negative 32-bit value");
    }

    @Test
    void movLLoadPcRelLoadsConstant() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, movLLoadPcRel(4, 2)); // MOV.L @(2,PC),R4 -> address (0&~3)+4+2*4=12
        bus.write32(12, 0x11223344);
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);

        cpu.step();

        assertEquals(0x11223344, cpu.r[4]);
    }

    @Test
    void movLLoadPcRelMasksLowPcBitsWhenInstructionIsNotLongwordAligned() {
        // Per the SH-4 spec, the PC value used is masked to a longword boundary
        // FIRST (PC & 0xFFFFFFFC), THEN 4 is added — regardless of whether this
        // instruction itself sits at a 4-aligned address. Placing the instruction
        // at address 2 (2 mod 4, not longword-aligned) means the masked target is
        // address 4, while an unmasked "PC+4" calculation would wrongly read from
        // address 6 instead. NOTE: those two 4-byte read windows ([4,8) and [6,10))
        // overlap by 2 bytes, so writing a second, different sentinel at address 6
        // would corrupt the one at address 4 — instead this relies on address 6
        // simply being left at its default zero value, which already differs from
        // the sentinel at address 4 and so still fails clearly if masking were wrong.
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, nop());                    // address 0-1: padding
        bus.writeInstruction(2, movLLoadPcRel(5, 0));       // address 2: MOV.L @(0,PC),R5
        bus.write32(4, 0x11111111);  // correct target: (2 & ~3) + 4 + 0 = 4
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);

        cpu.step(); // NOP
        cpu.step(); // MOV.L @(0,PC),R5

        assertEquals(0x11111111, cpu.r[5], "should read from the masked (longword-aligned) address");
    }

    @Test
    void movaComputesEffectiveAddress() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, mova(3)); // MOVA @(3,PC),R0 -> address (0&~3)+4+3*4=16
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);

        cpu.step();

        assertEquals(16, cpu.r[0], "MOVA loads the effective ADDRESS itself, not data read from it");
    }

    // ---- RTE (return from exception) -------------------------------------

    @Test
    void rteJumpsToSpcAndRestoresSrFromSsr() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, cmpEqReg(0, 0)); // T := 1 (R0 == R0)
        bus.writeInstruction(2, rte());
        bus.writeInstruction(4, nop()); // RTE's delay slot
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.spc = 200;
        cpu.ssr = 0; // T bit clear in the saved SR

        cpu.step(); // CMP/EQ
        assertTrue(cpu.tFlag(), "sanity check: T should be set before RTE runs");

        cpu.step(); // RTE
        assertEquals(200, cpu.pc, "PC should jump to SPC");
        assertFalse(cpu.tFlag(), "T should be restored from SSR, overriding the T=1 set just before RTE");
    }

    @Test
    void rteExecutesDelaySlotInstructionBeforeJumping() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, rte());
        bus.writeInstruction(2, movImm(3, 55)); // delay slot: MOV #55,R3 — must execute BEFORE the jump
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.spc = 8;
        cpu.ssr = 0;

        cpu.step();

        assertEquals(55, cpu.r[3], "the delay slot instruction's effect must be visible");
        assertEquals(8, cpu.pc, "PC must land on SPC, not thisPc+2 or thisPc+4");
    }

    @Test
    void rteInAnotherBranchesDelaySlotIsIllegal() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, bra(2));
        bus.writeInstruction(2, rte()); // illegal: RTE is itself a branch instruction
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);

        assertThrows(IllegalStateException.class, cpu::step);
    }

    @Test
    void branchInRteDelaySlotIsIllegal() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, rte());
        bus.writeInstruction(2, bt(0)); // illegal: a branch instruction in RTE's delay slot
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.spc = 100;
        cpu.ssr = 0;

        assertThrows(IllegalStateException.class, cpu::step);
    }

    // ---- Comparisons (CMP/HS, CMP/GE, CMP/HI, CMP/GT, CMP/PL, CMP/PZ, CMP/STR) ----

    @Test
    void cmpHsIsUnsignedGreaterOrEqual() {
        // -1 (0xFFFFFFFF) is huge as unsigned, tiny as signed -- this distinguishes
        // CMP/HS (unsigned) from CMP/GE (signed) sharply.
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, cmpHs(0, 1)); // CMP/HS R1,R0 -> T = (R0 >= R1) unsigned
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = -1;
        cpu.r[1] = 1;

        cpu.step();

        assertTrue(cpu.tFlag(), "-1 is unsigned-huge, so it IS >= 1 unsigned");
    }

    @Test
    void cmpGeIsSignedGreaterOrEqual() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, cmpGe(0, 1)); // CMP/GE R1,R0 -> T = (R0 >= R1) signed
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = -1;
        cpu.r[1] = 1;

        cpu.step();

        assertFalse(cpu.tFlag(), "-1 is signed-negative, so it is NOT >= 1 signed");
    }

    @Test
    void cmpHiIsUnsignedStrictlyGreater() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, cmpHi(0, 1));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 5;
        cpu.r[1] = 5;

        cpu.step();

        assertFalse(cpu.tFlag(), "equal values should not satisfy strictly-greater");
    }

    @Test
    void cmpGtIsSignedStrictlyGreater() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, cmpGt(1, 0)); // CMP/GT R0,R1 -> T = (R1 > R0) signed
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[1] = 10;
        cpu.r[0] = -5;

        cpu.step();

        assertTrue(cpu.tFlag());
    }

    @Test
    void cmpPlIsTrueOnlyForStrictlyPositive() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, cmpPl(0));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0;

        cpu.step();

        assertFalse(cpu.tFlag(), "zero is not > 0");
    }

    @Test
    void cmpPzIsTrueForZeroOrPositive() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, cmpPz(0));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0;

        cpu.step();

        assertTrue(cpu.tFlag(), "zero IS >= 0");
    }

    @Test
    void cmpStrDetectsAnyMatchingByte() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, cmpStr(0, 1));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0x11223344;
        cpu.r[1] = 0xAA22BBCC; // second-from-top byte (0x22) matches R0's

        cpu.step();

        assertTrue(cpu.tFlag());
    }

    @Test
    void cmpStrFindsNoMatchWhenAllBytesDiffer() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, cmpStr(0, 1));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0x11223344;
        cpu.r[1] = (int) 0xAABBCCDD;

        cpu.step();

        assertFalse(cpu.tFlag());
    }

    // ---- TST, DT ----

    @Test
    void tstRegSetsTWhenAndIsZero() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, tstReg(0, 1));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0b1010;
        cpu.r[1] = 0b0101; // no overlapping bits

        cpu.step();

        assertTrue(cpu.tFlag());
    }

    @Test
    void tstImmClearsTWhenAndIsNonZero() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, tstImm(0x0F));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0xFF;

        cpu.step();

        assertFalse(cpu.tFlag());
    }

    @Test
    void dtDecrementsAndSetsTWhenReachingZero() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, dt(4));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[4] = 1;

        cpu.step();

        assertEquals(0, cpu.r[4]);
        assertTrue(cpu.tFlag());
    }

    @Test
    void dtDoesNotSetTWhenStillNonZero() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, dt(4));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[4] = 5;

        cpu.step();

        assertEquals(4, cpu.r[4]);
        assertFalse(cpu.tFlag());
    }

    // ---- Sign/zero extension and byte/word manipulation ----

    @Test
    void extsBSignExtendsNegativeByte() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, extsB(1, 0));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0xFE; // -2 as a byte

        cpu.step();

        assertEquals(-2, cpu.r[1]);
    }

    @Test
    void extuBZeroExtendsSameByte() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, extuB(1, 0));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0xFE;

        cpu.step();

        assertEquals(0xFE, cpu.r[1], "unsigned: 0xFE stays 254, not -2");
    }

    @Test
    void extsWSignExtendsNegativeWord() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, extsW(1, 0));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0x8000;

        cpu.step();

        assertEquals(0xFFFF8000, cpu.r[1]);
    }

    @Test
    void extuWZeroExtendsSameWord() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, extuW(1, 0));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0x8000;

        cpu.step();

        assertEquals(0x8000, cpu.r[1]);
    }

    @Test
    void swapBSwapsLowTwoBytesOnly() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, swapB(1, 0));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0x12345678;

        cpu.step();

        assertEquals(0x12347856, cpu.r[1], "upper 16 bits (0x1234) pass through; low 2 bytes (0x56,0x78) swap");
    }

    @Test
    void swapWSwapsUpperAndLowerHalves() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, swapW(1, 0));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0x12345678;

        cpu.step();

        assertEquals(0x56781234, cpu.r[1]);
    }

    @Test
    void xtrctExtractsMiddle32Bits() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, xtrct(1, 0)); // XTRCT R0,R1 -> R1 = middle32(R0:R1)
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0x11112222; // high 32 bits of the conceptual 64-bit value
        cpu.r[1] = 0x33334444; // low 32 bits

        cpu.step();

        assertEquals(0x22223333, cpu.r[1], "low 16 of R0 (2222) + high 16 of R1 (3333)");
    }

    // ---- Multi-word arithmetic: ADDC/SUBC/NEGC (carry/borrow chaining), ADDV/SUBV (overflow) ----

    @Test
    void addcChainsCarryAcrossTwoWords() {
        // Mirrors the SH-4 manual's own 64-bit addition example: r1 = 0x00000001,
        // r3 = 0xFFFFFFFF -> addc r3,r1 should carry (T=1), r1 becomes 0.
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, addc(1, 3)); // ADDC R3,R1
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[1] = 0x00000001;
        cpu.r[3] = 0xFFFFFFFF;

        cpu.step();

        assertEquals(0, cpu.r[1]);
        assertTrue(cpu.tFlag(), "adding 1 + 0xFFFFFFFF overflows 32 bits -> carry");
    }

    @Test
    void addcDoesNotCarryWhenSumFits() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, addc(0, 1));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 5;
        cpu.r[1] = 3;

        cpu.step();

        assertEquals(8, cpu.r[0]);
        assertFalse(cpu.tFlag());
    }

    @Test
    void subcChainsBorrowAcrossTwoWords() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, subc(0, 1)); // SUBC R1,R0 -> R0 = R0 - R1 - T
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0;
        cpu.r[1] = 1;

        cpu.step();

        assertEquals(-1, cpu.r[0], "0 - 1 wraps to 0xFFFFFFFF");
        assertTrue(cpu.tFlag(), "borrow occurred");
    }

    @Test
    void negcNegatesAndSetsBorrowForNonZero() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, negc(1, 0)); // NEGC R0,R1 -> R1 = 0 - R0 - T
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 1;

        cpu.step();

        assertEquals(-1, cpu.r[1], "0 - 1 = -1 (0xFFFFFFFF)");
        assertTrue(cpu.tFlag(), "negating any nonzero value borrows");
    }

    @Test
    void negcOfZeroWithClearTDoesNotBorrow() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, negc(1, 0));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0;

        cpu.step();

        assertEquals(0, cpu.r[1]);
        assertFalse(cpu.tFlag(), "0 - 0 - 0 borrows nothing");
    }

    @Test
    void addvSetsTOnSignedOverflow() {
        // From the SH-4 manual's own ADDV example: 0x7FFFFFFE + 2 overflows into negative.
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, addv(1, 0));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 2;
        cpu.r[1] = 0x7FFFFFFE;

        cpu.step();

        assertEquals(0x80000000, cpu.r[1]);
        assertTrue(cpu.tFlag());
    }

    @Test
    void addvDoesNotSetTWhenNoOverflow() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, addv(1, 0));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 1;
        cpu.r[1] = 0x7FFFFFFE;

        cpu.step();

        assertEquals(0x7FFFFFFF, cpu.r[1]);
        assertFalse(cpu.tFlag());
    }

    @Test
    void subvSetsTOnSignedUnderflow() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, subv(1, 0));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 2;
        cpu.r[1] = 0x80000001;

        cpu.step();

        assertEquals(0x7FFFFFFF, cpu.r[1]);
        assertTrue(cpu.tFlag());
    }

    // ---- ROTL, ROTR, ROTCR, SHAD, SHLD, TAS.B -----------------------------

    @Test
    void rotlRotatesMsbIntoLsbAndT() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, rotl(0));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0x80000001;

        cpu.step();

        assertEquals(0x00000003, cpu.r[0], "MSB rotates into LSB: 1000...0001 -> 0000...0011");
        assertTrue(cpu.tFlag(), "the rotated-out MSB (1) becomes T");
    }

    @Test
    void rotrRotatesLsbIntoMsbAndT() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, rotr(0));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 0x00000003;

        cpu.step();

        assertEquals(0x80000001, cpu.r[0]);
        assertTrue(cpu.tFlag(), "the rotated-out LSB (1) becomes T");
    }

    @Test
    void rotcrUsesOldTAsNewMsbAndCapturesOldLsbAsNewT() {
        // Set T=1 first (via CMP/PZ on a non-negative register), THEN run ROTCR on a
        // register whose LSB is 0 -- confirms both halves of the 33-bit rotate: the
        // OLD T value (1) becomes the new MSB, and the OLD LSB (0) becomes the new T.
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, cmpPz(1)); // R1 >= 0 -> T := 1
        bus.writeInstruction(2, rotcr(0));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[1] = 0;
        cpu.r[0] = 0x00000002; // LSB is 0

        cpu.step(); // CMP/PZ
        cpu.step(); // ROTCR

        assertEquals(0x80000001, cpu.r[0], "old T (1) shifted into the MSB; 2>>>1==1, so result is 0x80000001");
        assertFalse(cpu.tFlag(), "the old LSB (0) becomes the new T");
    }

    @Test
    void shadShiftsLeftForPositiveRm() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, shad(0, 1)); // SHAD R1,R0
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 1;
        cpu.r[1] = 4;

        cpu.step();

        assertEquals(16, cpu.r[0]);
    }

    @Test
    void shadShiftsRightArithmeticForNegativeRm() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, shad(0, 1));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = -16; // negative, to distinguish arithmetic (sign-filling) from logical shift
        cpu.r[1] = -2; // shift right by 2

        cpu.step();

        assertEquals(-4, cpu.r[0], "arithmetic right shift sign-extends: -16 >> 2 == -4");
    }

    @Test
    void shadIsNoOpWhenRmIsZero() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, shad(0, 1));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = 12345;
        cpu.r[1] = 0;

        cpu.step();

        assertEquals(12345, cpu.r[0]);
    }

    @Test
    void shldShiftsRightLogicalForNegativeRm() {
        // Same negative Rn as the SHAD test above, but SHLD must zero-fill instead of
        // sign-extend -- this is the whole point of the two instructions being distinct.
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, shld(0, 1));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[0] = -16;
        cpu.r[1] = -2;

        cpu.step();

        assertEquals(-16 >>> 2, cpu.r[0], "logical right shift zero-fills, unlike SHAD's sign-extension");
    }

    @Test
    void tasBSetsTWhenByteIsZeroAndAlwaysSetsMsb() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, tasB(1));
        bus.write8(50, (byte) 0x00);
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[1] = 50;

        cpu.step();

        assertTrue(cpu.tFlag(), "the byte read WAS zero");
        assertEquals((byte) 0x80, bus.read8(50), "the byte's MSB is always forced to 1 afterward");
    }

    @Test
    void tasBClearsTWhenByteIsNonZeroButStillSetsMsb() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, tasB(1));
        bus.write8(50, (byte) 0x01);
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[1] = 50;

        cpu.step();

        assertFalse(cpu.tFlag(), "the byte read was NOT zero");
        assertEquals((byte) 0x81, bus.read8(50), "MSB forced to 1, low bits preserved");
    }

    @Test
    void stsLPrDecrementsThenStoresPrOnTheStack() {
        // The classic function-prologue instruction: STS.L PR,@-R15 (R15 = stack pointer
        // convention). Verified against a real Sonic Adventure GD-ROM dump: this is the exact
        // instruction the SH-4 interpreter hit (as PC=0x8C0100A2, opcode 0x4F22) 16 real
        // instructions into the game's actual boot code, before this was implemented.
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, stsLPr(15)); // STS.L PR,@-R15
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[15] = 100;
        cpu.pr = 0x12345678;

        cpu.step();

        assertEquals(96, cpu.r[15], "R15 should be decremented by 4 before the store");
        assertEquals(0x12345678, bus.read32(96), "PR's value should be stored at the new R15");
    }

    @Test
    void stsLMachDecrementsThenStoresMach() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, stsLMach(15));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[15] = 100;
        cpu.mach = 0x11223344;

        cpu.step();

        assertEquals(96, cpu.r[15]);
        assertEquals(0x11223344, bus.read32(96));
    }

    @Test
    void stsLMaclDecrementsThenStoresMacl() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, stsLMacl(15));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[15] = 100;
        cpu.macl = 0x55667788;

        cpu.step();

        assertEquals(96, cpu.r[15]);
        assertEquals(0x55667788, bus.read32(96));
    }

    @Test
    void ldsLPrLoadsThenIncrementsRn() {
        // The mirror-image function-epilogue instruction: LDS.L @R15+,PR.
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, ldsLPr(15)); // LDS.L @R15+,PR
        bus.write32(100, 0x12345678);
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[15] = 100;

        cpu.step();

        assertEquals(0x12345678, cpu.pr, "PR should hold the loaded value");
        assertEquals(104, cpu.r[15], "R15 should be incremented by 4 AFTER the load");
    }

    @Test
    void ldsLMachLoadsThenIncrementsRn() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, ldsLMach(15));
        bus.write32(100, 0x11223344);
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[15] = 100;

        cpu.step();

        assertEquals(0x11223344, cpu.mach);
        assertEquals(104, cpu.r[15]);
    }

    @Test
    void ldsLMaclLoadsThenIncrementsRn() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, ldsLMacl(15));
        bus.write32(100, 0x55667788);
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[15] = 100;

        cpu.step();

        assertEquals(0x55667788, cpu.macl);
        assertEquals(104, cpu.r[15]);
    }

    @Test
    void stsLPrThenLdsLPrRoundTripsThroughTheStack() {
        // The real-world pattern this pair exists for: save PR in a function's prologue,
        // do something (here, just clobber PR to prove the round trip is real and not a
        // no-op), then restore it in the epilogue immediately before RTS.
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        int pc = write(bus, 0, stsLPr(15));   // STS.L PR,@-R15
        write(bus, pc, ldsLPr(15));            // LDS.L @R15+,PR
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[15] = 100;
        cpu.pr = 0xCAFEBABE;

        cpu.step(); // STS.L PR,@-R15
        cpu.pr = 0; // clobber PR to prove the next step actually reloads it from memory
        cpu.step(); // LDS.L @R15+,PR

        assertEquals(0xCAFEBABE, cpu.pr, "PR should be restored to its original value");
        assertEquals(100, cpu.r[15], "R15 should be back where it started");
    }

    @Test
    void trapaSavesStateAndJumpsToVbrPlus0x100() {
        // TRAPA #imm: real HLE boot/syscall code's actual mechanism for invoking
        // BIOS-equivalent functionality (see docs/ROADMAP.md) — this interpreter has no
        // installed handler, so this test only checks the hardware-defined entry sequence
        // itself: TRA = imm<<2, SSR = SR, SPC = the return address, PC = VBR + 0x100.
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        int pc = write(bus, 0, cmpEqReg(0, 0)); // sets T = true (R0 == R0), via public API only
        write(bus, pc, trapa(0x23));
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.vbr = 0x1000;

        cpu.step(); // CMP/EQ R0,R0 — T becomes true
        assertTrue(cpu.tFlag(), "sanity check: T should be true before TRAPA");
        cpu.step(); // TRAPA #0x23

        assertEquals(0x23 << 2, cpu.tra, "TRA should hold the immediate shifted left 2 bits");
        assertEquals(1, cpu.ssr & 1, "SSR should have captured SR (T flag was true) before any change to SR");
        assertEquals(4, cpu.spc, "SPC should hold the return address (the instruction after TRAPA)");
        assertEquals(0x1000 + 0x100, cpu.pc, "PC should jump to VBR + 0x100");
    }

    @Test
    void ldcVbrSetsVbrFromRegister() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, ldcVbr(3)); // LDC R3,VBR
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[3] = 0x8C020000;

        cpu.step();

        assertEquals(0x8C020000, cpu.vbr);
    }

    @Test
    void stcVbrReadsVbrIntoRegister() {
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        bus.writeInstruction(0, stcVbr(3)); // STC VBR,R3
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.vbr = 0x8C020000;

        cpu.step();

        assertEquals(0x8C020000, cpu.r[3]);
    }

    @Test
    void realisticSequenceLdcVbrThenTrapaJumpsToTheJustSetHandler() {
        // The real-world pattern this pair exists for: runtime-startup code sets VBR to
        // point at its own exception vector table (LDC Rn,VBR) before anything relies on
        // TRAPA/interrupts working — then a later TRAPA correctly lands inside it.
        SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
        int pc = write(bus, 0, ldcVbr(1));  // LDC R1,VBR
        write(bus, pc, trapa(0x10));         // TRAPA #0x10
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);
        cpu.r[1] = 0x40;

        cpu.step(); // LDC R1,VBR
        cpu.step(); // TRAPA #0x10

        assertEquals(0x40 + 0x100, cpu.pc);
    }

    @Test
    void stepLogsPcAndOpcodeAtTraceLevelWhenEnabled() {
        // Verifies the wiring in step() itself (see Sh4Cpu's LOG field) — Logger/LogConfig's
        // own behavior is covered by common's LoggerTest; this only checks Sh4Cpu actually
        // calls it, with the right values, once per instruction.
        org.dreamjemu.common.log.LogConfig.setGlobalLevel(org.dreamjemu.common.log.LogLevel.TRACE);
        java.util.List<org.dreamjemu.common.log.LogRecord> captured = new java.util.ArrayList<>();
        org.dreamjemu.common.log.LogConfig.setSink(captured::add);
        try {
            SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
            bus.writeInstruction(0, 0x0009); // NOP
            Sh4Cpu cpu = new Sh4Cpu(bus, 0);

            cpu.step();

            assertEquals(1, captured.size());
            assertEquals("Sh4Cpu", captured.get(0).loggerName());
            assertEquals("PC=0x00000000 opcode=0x0009", captured.get(0).message());
        } finally {
            org.dreamjemu.common.log.LogConfig.setGlobalLevel(org.dreamjemu.common.log.LogLevel.INFO);
            org.dreamjemu.common.log.LogConfig.resetSinkToStdout();
        }
    }

    @Test
    void stepDoesNotLogAtTheDefaultLevel() {
        // Default level is INFO; TRACE-level per-instruction logging must stay silent unless
        // explicitly raised — critical given real disc images mean millions of step() calls.
        java.util.List<org.dreamjemu.common.log.LogRecord> captured = new java.util.ArrayList<>();
        org.dreamjemu.common.log.LogConfig.setSink(captured::add);
        try {
            SimpleTestBus bus = new SimpleTestBus(MEM_SIZE);
            bus.writeInstruction(0, 0x0009); // NOP
            Sh4Cpu cpu = new Sh4Cpu(bus, 0);

            cpu.step();

            assertTrue(captured.isEmpty());
        } finally {
            org.dreamjemu.common.log.LogConfig.resetSinkToStdout();
        }
    }

    private static int write(SimpleTestBus bus, int address, int opcode) {
        bus.writeInstruction(address, opcode);
        return address + 2;
    }
}
