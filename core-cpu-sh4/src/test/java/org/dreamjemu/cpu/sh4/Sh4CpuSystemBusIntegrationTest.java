package org.dreamjemu.cpu.sh4;

import org.dreamjemu.system.DreamcastAddressMap;
import org.dreamjemu.system.SystemBus;
import org.junit.jupiter.api.Test;

import static org.dreamjemu.cpu.sh4.Sh4Asm.addImm;
import static org.dreamjemu.cpu.sh4.Sh4Asm.addReg;
import static org.dreamjemu.cpu.sh4.Sh4Asm.bf;
import static org.dreamjemu.cpu.sh4.Sh4Asm.cmpEqReg;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movImm;
import static org.dreamjemu.cpu.sh4.Sh4Asm.movLStore;
import static org.dreamjemu.cpu.sh4.Sh4Asm.nop;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration tests wiring {@link Sh4Cpu} to core-system's real
 * {@link SystemBus} — as opposed to {@link Sh4CpuTest}, which uses the
 * trivial {@link SimpleTestBus} to test the interpreter in isolation.
 *
 * These exercise real Dreamcast physical addresses: main RAM, its SH-4
 * cache-area mirrors (P1/P2, i.e. the 0x80000000/0xA0000000 address
 * ranges), and the VRAM/AICA RAM placeholder regions reserved for
 * core-gpu-pvr2/core-aica — confirming the CPU and the system bus work
 * together correctly, and that touching not-yet-implemented peripheral
 * regions is safe (no crash) rather than silently wrong.
 *
 * The immediate-load instruction this interpreter supports so far
 * (MOV #imm,Rn) can only load an 8-bit sign-extended value, so these test
 * programs pre-seed any register that needs a full 32-bit address (like a
 * memory-store target) directly via {@code cpu.r[n] = ...} before running,
 * rather than trying to synthesize a wide constant in hand-assembled code.
 */
class Sh4CpuSystemBusIntegrationTest {

    /** Writes a hand-assembled program (16-bit opcodes) sequentially starting at baseAddress. */
    private static void loadProgram(SystemBus bus, long baseAddress, int... opcodes) {
        for (int i = 0; i < opcodes.length; i++) {
            bus.write16(baseAddress + i * 2L, (short) opcodes[i]);
        }
    }

    /**
     * Same loop program as Sh4CpuTest's hand-assembled integration test
     * (sum 5+4+3+2+1 via real conditional branching), but running against
     * real main RAM through core-system's SystemBus instead of a trivial
     * test bus, with the target store address pre-seeded in R4 (see class
     * Javadoc for why).
     *
     * Program layout (offsets from base):
     *   0: MOV #0,  R0        ; accumulator = 0
     *   2: MOV #5,  R1        ; counter = 5
     *   4: MOV #0,  R2        ; zero constant, for the loop exit comparison
     *   6: ADD R1,R0          ; R0 += R1              <- loop start
     *   8: ADD #-1,R1         ; R1 -= 1
     *   10: CMP/EQ R2,R1      ; T = (R1 == 0)
     *   12: BF -5             ; if R1 != 0, branch back to offset 6
     *   14: MOV.L R0,@R4      ; store final sum to memory (R4 pre-seeded)
     *   16: NOP                ; end marker
     */
    private static void loadLoopProgram(SystemBus bus, long base) {
        loadProgram(bus, base,
                movImm(0, 0),
                movImm(1, 5),
                movImm(2, 0),
                addReg(0, 1),
                addImm(1, -1),
                cmpEqReg(1, 2),
                bf(-5),
                movLStore(4, 0),
                nop()
        );
    }

    private static int runUntil(Sh4Cpu cpu, int endAddress) {
        int steps = 0;
        while (cpu.pc != endAddress) {
            cpu.step();
            steps++;
            if (steps > 1000) {
                throw new AssertionError("Program did not terminate — likely an infinite loop");
            }
        }
        return steps;
    }

    @Test
    void loopProgramRunsCorrectlyAgainstRealMainRam() {
        SystemBus bus = new SystemBus();
        long base = DreamcastAddressMap.MAIN_RAM_BASE;
        loadLoopProgram(bus, base);

        Sh4Cpu cpu = new Sh4Cpu(bus, (int) base);
        long storeAddress = base + 0x40; // well past the program's own instructions
        cpu.r[4] = (int) storeAddress;

        int steps = runUntil(cpu, (int) base + 16);

        assertEquals(15, cpu.r[0], "R0 should hold the sum 5+4+3+2+1");
        assertEquals(15, bus.read32(storeAddress), "the sum should also be readable back from real main RAM");
        assertEquals(24, steps);
    }

    @Test
    void loopProgramRunsIdenticallyWhenExecutedThroughACacheAreaMirror() {
        // Boots and runs the exact same program, but through the SH-4's P2
        // (0xA0000000-based) cache-area mirror instead of the raw physical
        // base address. SystemBus must mask the top address bits on every
        // fetch for this to reach the same instructions and RAM — this is
        // the CPU-level counterpart to SystemBusTest's
        // cacheAreaBitsAreMaskedToTheSamePhysicalMemory test.
        SystemBus bus = new SystemBus();
        long base = DreamcastAddressMap.MAIN_RAM_BASE;
        loadLoopProgram(bus, base);

        long mirroredBase = 0xA0000000L | base;
        Sh4Cpu cpu = new Sh4Cpu(bus, (int) mirroredBase);
        long storeAddress = base + 0x80; // plain physical address; the store target doesn't need to be mirrored
        cpu.r[4] = (int) storeAddress;

        int steps = runUntil(cpu, (int) mirroredBase + 16);

        assertEquals(15, cpu.r[0], "R0 should hold the sum 5+4+3+2+1 even when fetched through a cache-area mirror");
        assertEquals(15, bus.read32(storeAddress), "the sum should be visible at the plain physical address too");
        assertEquals(24, steps);
    }

    @Test
    void cpuCanSafelyTouchTheUnmappedVramPlaceholderRegion() {
        // core-gpu-pvr2 doesn't exist yet, so VRAM is currently an
        // UnmappedRegion placeholder (see core-system's SystemBus). This
        // confirms the CPU can store to and load from that region without
        // crashing -- writes are safely discarded and reads return 0 -- so
        // early HLE/bring-up code that pokes at not-yet-implemented
        // peripherals doesn't take the whole system down.
        SystemBus bus = new SystemBus();
        long base = DreamcastAddressMap.MAIN_RAM_BASE;
        loadProgram(bus, base,
                movImm(0, 42),   // R0 = 42
                movLStore(1, 0), // store R0 to the address in R1
                nop()
        );

        Sh4Cpu cpu = new Sh4Cpu(bus, (int) base);
        cpu.r[1] = (int) DreamcastAddressMap.VRAM_BASE;

        runUntil(cpu, (int) base + 4);

        assertEquals(42, cpu.r[0]);
        assertEquals(0, bus.read32(DreamcastAddressMap.VRAM_BASE),
                "VRAM is an unmapped placeholder for now; writes are safely ignored, reads return 0");
    }
}
