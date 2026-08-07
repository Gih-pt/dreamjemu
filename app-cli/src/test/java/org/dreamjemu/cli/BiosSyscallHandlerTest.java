package org.dreamjemu.cli;

import org.dreamjemu.cpu.sh4.Sh4Cpu;
import org.dreamjemu.system.Bus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link BiosSyscallHandler} in isolation, against a minimal in-memory {@link Bus} —
 * doesn't need a real disc image or boot file, since this class only touches CPU registers and
 * the 4 documented vector addresses.
 */
class BiosSyscallHandlerTest {

    private static final class MemBus implements Bus {
        final byte[] mem = new byte[0x2000];

        private int offset(long address) {
            // Trap/vector addresses used in this test are all within 0x8C000000-0x8C0001FF;
            // mask down to fit this small backing array.
            return (int) (address & 0x1FFF);
        }

        public byte read8(long a) {
            return mem[offset(a)];
        }

        public short read16(long a) {
            int i = offset(a);
            return (short) ((mem[i] & 0xFF) | ((mem[i + 1] & 0xFF) << 8));
        }

        public int read32(long a) {
            int i = offset(a);
            return (mem[i] & 0xFF) | ((mem[i + 1] & 0xFF) << 8) | ((mem[i + 2] & 0xFF) << 16) | ((mem[i + 3] & 0xFF) << 24);
        }

        public long read64(long a) {
            long lo = read32(a) & 0xFFFFFFFFL;
            long hi = read32(a + 4) & 0xFFFFFFFFL;
            return lo | (hi << 32);
        }

        public void write8(long a, byte v) {
            mem[offset(a)] = v;
        }

        public void write16(long a, short v) {
            int i = offset(a);
            mem[i] = (byte) (v & 0xFF);
            mem[i + 1] = (byte) ((v >> 8) & 0xFF);
        }

        public void write32(long a, int v) {
            int i = offset(a);
            mem[i] = (byte) (v & 0xFF);
            mem[i + 1] = (byte) ((v >> 8) & 0xFF);
            mem[i + 2] = (byte) ((v >> 16) & 0xFF);
            mem[i + 3] = (byte) ((v >> 24) & 0xFF);
        }

        public void write64(long a, long v) {
            write32(a, (int) (v & 0xFFFFFFFFL));
            write32(a + 4, (int) ((v >>> 32) & 0xFFFFFFFFL));
        }
    }

    private static Sh4Cpu cpuAt(Bus bus, int pc) {
        Sh4Cpu cpu = new Sh4Cpu(bus, pc);
        cpu.pr = 0x8C010000; // an arbitrary, recognizable "return here" address for every test
        return cpu;
    }

    @Test
    void installVectorTableWritesAllFourTrapAddressesIntoTheDocumentedVectors() {
        MemBus bus = new MemBus();

        BiosSyscallHandler.installVectorTable(bus);

        assertTrue(BiosSyscallHandler.isSyscallTrap(bus.read32(BiosSyscallHandler.VECTOR_SYSINFO)));
        assertTrue(BiosSyscallHandler.isSyscallTrap(bus.read32(BiosSyscallHandler.VECTOR_ROMFONT)));
        assertTrue(BiosSyscallHandler.isSyscallTrap(bus.read32(BiosSyscallHandler.VECTOR_FLASHROM)));
        assertTrue(BiosSyscallHandler.isSyscallTrap(bus.read32(BiosSyscallHandler.VECTOR_MISC_GDROM)));
    }

    @Test
    void isSyscallTrapIsFalseForAnOrdinaryAddress() {
        assertFalse(BiosSyscallHandler.isSyscallTrap(0x8C010000));
    }

    @Test
    void sysinfoInitReportsSuccess() {
        MemBus bus = new MemBus();
        BiosSyscallHandler.installVectorTable(bus);
        Sh4Cpu cpu = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_SYSINFO));
        cpu.r[7] = 0; // SYSINFO_INIT

        BiosSyscallHandler.handle(cpu);

        assertEquals(0, cpu.r[0]);
        assertEquals(cpu.pr, cpu.pc, "handle() must return to PR, like a real syscall's own RTS would");
    }

    @Test
    void sysinfoIconReportsFailure() {
        MemBus bus = new MemBus();
        BiosSyscallHandler.installVectorTable(bus);
        Sh4Cpu cpu = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_SYSINFO));
        cpu.r[7] = 2; // SYSINFO_ICON

        BiosSyscallHandler.handle(cpu);

        assertEquals(-1, cpu.r[0]);
    }

    @Test
    void romfontUsesR1NotR7ToSelectItsFunction() {
        MemBus bus = new MemBus();
        BiosSyscallHandler.installVectorTable(bus);
        Sh4Cpu cpu = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_ROMFONT));
        cpu.r[7] = 99; // must be ignored — ROMFONT is the documented r1 exception
        cpu.r[1] = 1;  // ROMFONT_LOCK

        BiosSyscallHandler.handle(cpu);

        assertEquals(0, cpu.r[0], "ROMFONT_LOCK (selected via r1) should have run, not whatever r7=99 would be");
    }

    @Test
    void flashromAlwaysReportsFailureForEveryDocumentedFunction() {
        for (int fn = 0; fn <= 3; fn++) {
            MemBus bus = new MemBus();
            BiosSyscallHandler.installVectorTable(bus);
            Sh4Cpu cpu = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_FLASHROM));
            cpu.r[7] = fn;

            BiosSyscallHandler.handle(cpu);

            assertEquals(-1, cpu.r[0], "FLASHROM function " + fn + " should honestly report failure (no flashrom emulation)");
        }
    }

    @Test
    void miscInitReportsSuccess() {
        MemBus bus = new MemBus();
        BiosSyscallHandler.installVectorTable(bus);
        Sh4Cpu cpu = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_MISC_GDROM));
        cpu.r[6] = -1; // MISC superfunction
        cpu.r[7] = 0;  // MISC_INIT

        BiosSyscallHandler.handle(cpu);

        assertEquals(0, cpu.r[0]);
    }

    @Test
    void gdromFunctionsReportFailureHonestly() {
        MemBus bus = new MemBus();
        BiosSyscallHandler.installVectorTable(bus);
        Sh4Cpu cpu = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_MISC_GDROM));
        cpu.r[6] = 0; // GDROM superfunction
        cpu.r[7] = 3; // GDROM_SEND_COMMAND (arbitrary — none are implemented yet)

        BiosSyscallHandler.handle(cpu);

        assertEquals(-1, cpu.r[0]);
    }

    @Test
    void handleThrowsForAnAddressThatIsNotATrap() {
        MemBus bus = new MemBus();
        Sh4Cpu cpu = cpuAt(bus, 0x8C010000);

        assertThrows(IllegalArgumentException.class, () -> BiosSyscallHandler.handle(cpu));
    }
}
