package org.dreamjemu.cpu.sh4;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Sh4CpuTest {

    private static final int MEM_SIZE = 256;

    // --- Instruction encoders, mirroring the SH-4 ISA formats implemented in Sh4Cpu ---

    private static int movImm(int n, int imm8) {
        return 0xE000 | (n << 8) | (imm8 & 0xFF);
    }

    private static int movReg(int n, int m) {
        return 0x6003 | (n << 8) | (m << 4);
    }

    private static int addImm(int n, int imm8) {
        return 0x7000 | (n << 8) | (imm8 & 0xFF);
    }

    private static int addReg(int n, int m) {
        return 0x300C | (n << 8) | (m << 4);
    }

    private static int subReg(int n, int m) {
        return 0x3008 | (n << 8) | (m << 4);
    }

    private static int cmpEqReg(int n, int m) {
        return 0x3000 | (n << 8) | (m << 4);
    }

    private static int cmpEqImmR0(int imm8) {
        return 0x8800 | (imm8 & 0xFF);
    }

    private static int bt(int disp8) {
        return 0x8900 | (disp8 & 0xFF);
    }

    private static int bf(int disp8) {
        return 0x8B00 | (disp8 & 0xFF);
    }

    private static int bra(int disp12) {
        return 0xA000 | (disp12 & 0x0FFF);
    }

    private static int movLStore(int n, int m) {
        return 0x2002 | (n << 8) | (m << 4); // MOV.L Rm,@Rn
    }

    private static int movLLoad(int n, int m) {
        return 0x6002 | (n << 8) | (m << 4); // MOV.L @Rm,Rn
    }

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
        bus.writeInstruction(0, bra(2)); // target = 0 + 4 + 2*2 = 8
        Sh4Cpu cpu = new Sh4Cpu(bus, 0);

        cpu.step();
        assertEquals(8, cpu.pc);

        // Negative displacement (backward branch)
        bus.writeInstruction(20, bra(-5)); // target = 20 + 4 + (-5*2) = 14
        cpu.pc = 20;
        cpu.step();
        assertEquals(14, cpu.pc);
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
}
