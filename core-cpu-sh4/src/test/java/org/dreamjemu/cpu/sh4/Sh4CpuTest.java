package org.dreamjemu.cpu.sh4;

import org.junit.jupiter.api.Test;

import static org.dreamjemu.cpu.sh4.Sh4Asm.addImm;
import static org.dreamjemu.cpu.sh4.Sh4Asm.addReg;
import static org.dreamjemu.cpu.sh4.Sh4Asm.andImmR0;
import static org.dreamjemu.cpu.sh4.Sh4Asm.andReg;
import static org.dreamjemu.cpu.sh4.Sh4Asm.bf;
import static org.dreamjemu.cpu.sh4.Sh4Asm.bra;
import static org.dreamjemu.cpu.sh4.Sh4Asm.bsr;
import static org.dreamjemu.cpu.sh4.Sh4Asm.bt;
import static org.dreamjemu.cpu.sh4.Sh4Asm.cmpEqImmR0;
import static org.dreamjemu.cpu.sh4.Sh4Asm.cmpEqReg;
import static org.dreamjemu.cpu.sh4.Sh4Asm.jsr;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movImm;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movLLoad;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movLStore;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movReg;
import static org.dreamjemu.cpu.sh4.Sh4Asm.nop;
import static org.dreamjemu.cpu.sh4.Sh4Asm.orImmR0;
import static org.dreamjemu.cpu.sh4.Sh4Asm.orReg;
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
}
