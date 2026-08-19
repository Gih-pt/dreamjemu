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
        BiosSyscallHandler handler = new BiosSyscallHandler(bus);
        Sh4Cpu cpu = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_SYSINFO));
        cpu.r[7] = 0; // SYSINFO_INIT

        handler.handle(cpu);

        assertEquals(0, cpu.r[0]);
        assertEquals(cpu.pr, cpu.pc, "handle() must return to PR, like a real syscall's own RTS would");
    }

    @Test
    void sysinfoIconReportsFailure() {
        MemBus bus = new MemBus();
        BiosSyscallHandler.installVectorTable(bus);
        BiosSyscallHandler handler = new BiosSyscallHandler(bus);
        Sh4Cpu cpu = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_SYSINFO));
        cpu.r[7] = 2; // SYSINFO_ICON

        handler.handle(cpu);

        assertEquals(-1, cpu.r[0]);
    }

    @Test
    void romfontUsesR1NotR7ToSelectItsFunction() {
        MemBus bus = new MemBus();
        BiosSyscallHandler.installVectorTable(bus);
        BiosSyscallHandler handler = new BiosSyscallHandler(bus);
        Sh4Cpu cpu = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_ROMFONT));
        cpu.r[7] = 99; // must be ignored — ROMFONT is the documented r1 exception
        cpu.r[1] = 1;  // ROMFONT_LOCK

        handler.handle(cpu);

        assertEquals(0, cpu.r[0], "ROMFONT_LOCK (selected via r1) should have run, not whatever r7=99 would be");
    }

    @Test
    void flashromInfoReturnsRealPartitionOffsetsAndSizes() {
        // Confirmed against mc.pp.se/dc/syscalls.html's own worked partition table.
        MemBus bus = new MemBus();
        BiosSyscallHandler.installVectorTable(bus);
        BiosSyscallHandler handler = new BiosSyscallHandler(bus);
        Sh4Cpu cpu = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_FLASHROM));
        cpu.r[7] = 0; // FLASHROM_INFO
        cpu.r[4] = 0; // partition 0 (factory settings)
        cpu.r[5] = 0x8C000200; // output: two int32s (offset, size)

        handler.handle(cpu);

        assertEquals(0, cpu.r[0]);
        assertEquals(0x1A000, bus.read32(0x8C000200));
        assertEquals(0x2000, bus.read32(0x8C000204));
    }

    @Test
    void flashromInfoFailsForAnInvalidPartition() {
        MemBus bus = new MemBus();
        BiosSyscallHandler.installVectorTable(bus);
        BiosSyscallHandler handler = new BiosSyscallHandler(bus);
        Sh4Cpu cpu = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_FLASHROM));
        cpu.r[7] = 0; // FLASHROM_INFO
        cpu.r[4] = 5; // only 0-4 are real partitions

        handler.handle(cpu);

        assertEquals(-1, cpu.r[0]);
    }

    @Test
    void flashromReadReturnsHonestlyBlankErasedBytesAndReportsSuccess() {
        // This project has no real dumped flash.bin, so the only honest content it can offer is
        // real NOR flash's own documented erased-cell value (0xFF) — see BiosSyscallHandler's
        // own flashRom Javadoc.
        MemBus bus = new MemBus();
        BiosSyscallHandler.installVectorTable(bus);
        BiosSyscallHandler handler = new BiosSyscallHandler(bus);
        Sh4Cpu cpu = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_FLASHROM));
        cpu.r[7] = 1; // FLASHROM_READ
        cpu.r[4] = 0x1A000; // partition 0's own start offset
        cpu.r[5] = 0x8C000300; // dest
        cpu.r[6] = 4; // count

        handler.handle(cpu);

        assertEquals(4, cpu.r[0], "must report the real byte count read");
        assertEquals((byte) 0xFF, bus.read8(0x8C000300));
        assertEquals((byte) 0xFF, bus.read8(0x8C000303));
    }

    @Test
    void flashromReadFailsForAnOutOfRangeRequest() {
        MemBus bus = new MemBus();
        BiosSyscallHandler.installVectorTable(bus);
        BiosSyscallHandler handler = new BiosSyscallHandler(bus);
        Sh4Cpu cpu = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_FLASHROM));
        cpu.r[7] = 1; // FLASHROM_READ
        cpu.r[4] = 0x1FFFC;
        cpu.r[5] = 0x8C000300;
        cpu.r[6] = 100; // would run past the real 128KB chip's end

        handler.handle(cpu);

        assertEquals(-1, cpu.r[0]);
    }

    @Test
    void flashromWriteOnlyEverClearsBitsMatchingRealNorFlash() {
        // Real NOR flash without a prior erase can only ever clear bits (1 -> 0), never set them
        // — confirmed against mc.pp.se and redream's own flash.c.
        MemBus bus = new MemBus();
        BiosSyscallHandler.installVectorTable(bus);
        BiosSyscallHandler handler = new BiosSyscallHandler(bus);

        Sh4Cpu cpu = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_FLASHROM));
        cpu.r[7] = 2; // FLASHROM_WRITE
        cpu.r[4] = 0x1A000;
        cpu.r[5] = 0x8C000400;
        bus.write8(0x8C000400, (byte) 0x0F);
        cpu.r[6] = 1;
        handler.handle(cpu);
        assertEquals(1, cpu.r[0]);

        // Read it back: 0xFF (erased) & 0x0F (written) = 0x0F.
        Sh4Cpu cpu2 = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_FLASHROM));
        cpu2.r[7] = 1; // FLASHROM_READ
        cpu2.r[4] = 0x1A000;
        cpu2.r[5] = 0x8C000500;
        cpu2.r[6] = 1;
        handler.handle(cpu2);
        assertEquals((byte) 0x0F, bus.read8(0x8C000500), "0xFF & 0x0F must leave only the cleared bits");
    }

    @Test
    void flashromWriteFailsForAnOutOfRangeRequest() {
        MemBus bus = new MemBus();
        BiosSyscallHandler.installVectorTable(bus);
        BiosSyscallHandler handler = new BiosSyscallHandler(bus);
        Sh4Cpu cpu = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_FLASHROM));
        cpu.r[7] = 2; // FLASHROM_WRITE
        cpu.r[4] = -5; // negative offset, never valid
        cpu.r[5] = 0x8C000400;
        cpu.r[6] = 1;

        handler.handle(cpu);

        assertEquals(-1, cpu.r[0]);
    }

    @Test
    void flashromDeleteErasesTheWholePartitionBackToBlank() {
        MemBus bus = new MemBus();
        BiosSyscallHandler.installVectorTable(bus);
        BiosSyscallHandler handler = new BiosSyscallHandler(bus);

        // First clear a bit via a real write, confirming it's genuinely 0 beforehand.
        Sh4Cpu writeCpu = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_FLASHROM));
        writeCpu.r[7] = 2;
        writeCpu.r[4] = 0x1A000;
        writeCpu.r[5] = 0x8C000400;
        bus.write8(0x8C000400, (byte) 0x00);
        writeCpu.r[6] = 1;
        handler.handle(writeCpu);

        Sh4Cpu deleteCpu = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_FLASHROM));
        deleteCpu.r[7] = 3; // FLASHROM_DELETE
        deleteCpu.r[4] = 0x1A000; // partition 0's own start offset

        handler.handle(deleteCpu);

        assertEquals(0, deleteCpu.r[0]);

        Sh4Cpu readCpu = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_FLASHROM));
        readCpu.r[7] = 1;
        readCpu.r[4] = 0x1A000;
        readCpu.r[5] = 0x8C000500;
        readCpu.r[6] = 1;
        handler.handle(readCpu);
        assertEquals((byte) 0xFF, bus.read8(0x8C000500), "the deleted partition must read back as erased (0xFF)");
    }

    @Test
    void flashromDeleteFailsWhenTheOffsetIsNotAPartitionsOwnStart() {
        MemBus bus = new MemBus();
        BiosSyscallHandler.installVectorTable(bus);
        BiosSyscallHandler handler = new BiosSyscallHandler(bus);
        Sh4Cpu cpu = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_FLASHROM));
        cpu.r[7] = 3; // FLASHROM_DELETE
        cpu.r[4] = 0x1A001; // one byte into partition 0, not its start

        handler.handle(cpu);

        assertEquals(-1, cpu.r[0]);
    }

    @Test
    void sysinfoIdReturnsARealNonNullPointerAfterInit() {
        // Real hardware documents no failure return value for SYSINFO_ID at all — confirms this
        // no longer returns the null pointer it used to.
        MemBus bus = new MemBus();
        BiosSyscallHandler.installVectorTable(bus);
        BiosSyscallHandler handler = new BiosSyscallHandler(bus);

        Sh4Cpu initCpu = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_SYSINFO));
        initCpu.r[7] = 0; // SYSINFO_INIT
        handler.handle(initCpu);

        Sh4Cpu idCpu = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_SYSINFO));
        idCpu.r[7] = 3; // SYSINFO_ID
        handler.handle(idCpu);

        assertEquals(0x8C000068, idCpu.r[0], "must return the real, documented, fixed SYSINFO data address");
        assertEquals((byte) 0xFF, bus.read8(idCpu.r[0]), "the pointed-to data must be real (if honestly blank) flashrom bytes, not garbage");
    }

    @Test
    void miscInitReportsSuccess() {
        MemBus bus = new MemBus();
        BiosSyscallHandler.installVectorTable(bus);
        BiosSyscallHandler handler = new BiosSyscallHandler(bus);
        Sh4Cpu cpu = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_MISC_GDROM));
        cpu.r[6] = -1; // MISC superfunction
        cpu.r[7] = 0;  // MISC_INIT

        handler.handle(cpu);

        assertEquals(0, cpu.r[0]);
    }

    @Test
    void gdromInitReportsSuccessAndResetsQueueState() {
        // GDROM_INIT (r7=3, the syscall that resets the queue) — distinct from GDC_INIT (a
        // queued *command*, r4=0x18) — is control-only and needs no real disc data, so it should
        // succeed for real, not join the "honestly reports failure" group below.
        MemBus bus = new MemBus();
        BiosSyscallHandler.installVectorTable(bus);
        BiosSyscallHandler handler = new BiosSyscallHandler(bus);
        Sh4Cpu cpu = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_MISC_GDROM));
        cpu.r[6] = 0; // GDROM superfunction
        cpu.r[7] = 3; // GDROM_INIT

        handler.handle(cpu);

        assertEquals(0, cpu.r[0]);
    }

    @Test
    void gdromSendCommandOfAControlOnlyCommandCompletesSuccessfully() {
        MemBus bus = new MemBus();
        BiosSyscallHandler.installVectorTable(bus);
        BiosSyscallHandler handler = new BiosSyscallHandler(bus);
        Sh4Cpu cpu = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_MISC_GDROM));
        cpu.r[6] = 0;      // GDROM superfunction
        cpu.r[7] = 0;      // GDROM_SEND_COMMAND
        cpu.r[4] = 0x21;   // GDC_STOP — control-only, no disc data needed

        handler.handle(cpu);
        int requestId = cpu.r[0];

        assertTrue(requestId >= 1, "SEND_COMMAND must return a request id >= 1 on success");

        // Now check on it — a control-only command should report COMPLETE (0x2) with no error.
        cpu.pc = BiosSyscallHandler.VECTOR_MISC_GDROM; // arbitrary trap re-entry, handle() re-dispatches from pc
        cpu.pc = bus.read32(BiosSyscallHandler.VECTOR_MISC_GDROM);
        cpu.r[7] = 1; // GDROM_CHECK_COMMAND
        cpu.r[4] = requestId;
        cpu.r[5] = 0; // no extended-status buffer this time — must not crash

        handler.handle(cpu);

        assertEquals(0x2, cpu.r[0], "a control-only command must report GDC_STATUS_COMPLETE");
    }

    @Test
    void gdromSendCommandOfPioreadReportsFailureHonestly() {
        // GDC_PIOREAD needs a disc-absolute FAD -> track-relative LBA conversion this project
        // doesn't have a confirmed offset for yet (see BiosSyscallHandler.handleGdrom's Javadoc)
        // -- it must keep honestly failing, not silently return garbage sector data.
        MemBus bus = new MemBus();
        BiosSyscallHandler.installVectorTable(bus);
        BiosSyscallHandler handler = new BiosSyscallHandler(bus);
        Sh4Cpu cpu = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_MISC_GDROM));
        cpu.r[6] = 0;      // GDROM superfunction
        cpu.r[7] = 0;      // GDROM_SEND_COMMAND
        cpu.r[4] = 0x10;   // GDC_PIOREAD

        handler.handle(cpu);
        int requestId = cpu.r[0];
        assertTrue(requestId >= 1, "SEND_COMMAND itself still succeeds (the command was accepted/enqueued)");

        cpu.pc = bus.read32(BiosSyscallHandler.VECTOR_MISC_GDROM);
        cpu.r[7] = 1; // GDROM_CHECK_COMMAND
        cpu.r[4] = requestId;
        cpu.r[5] = 0;

        handler.handle(cpu);

        assertEquals(-1, cpu.r[0], "GDC_PIOREAD's outcome must honestly report GDC_STATUS_ERROR, not fabricate success");
    }

    @Test
    void gdromCheckCommandWritesExtendedStatusBlockToProvidedAddress() {
        MemBus bus = new MemBus();
        BiosSyscallHandler.installVectorTable(bus);
        BiosSyscallHandler handler = new BiosSyscallHandler(bus);
        Sh4Cpu cpu = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_MISC_GDROM));
        cpu.r[6] = 0;
        cpu.r[7] = 0;    // SEND_COMMAND
        cpu.r[4] = 0x18; // GDC_INIT — control-only

        handler.handle(cpu);
        int requestId = cpu.r[0];

        cpu.pc = bus.read32(BiosSyscallHandler.VECTOR_MISC_GDROM);
        cpu.r[7] = 1; // CHECK_COMMAND
        cpu.r[4] = requestId;
        int statusAddr = 0x8C000180;
        cpu.r[5] = statusAddr;

        handler.handle(cpu);

        assertEquals(0, bus.read32(statusAddr), "first int is the generic error code — 0 (OK) for a successful command");
    }

    @Test
    void gdromCheckCommandWithAMismatchedRequestIdReportsError() {
        MemBus bus = new MemBus();
        BiosSyscallHandler.installVectorTable(bus);
        BiosSyscallHandler handler = new BiosSyscallHandler(bus);
        Sh4Cpu cpu = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_MISC_GDROM));
        cpu.r[6] = 0;
        cpu.r[7] = 1;     // CHECK_COMMAND, with no command ever sent first
        cpu.r[4] = 12345; // an id nothing ever issued
        cpu.r[5] = 0;

        handler.handle(cpu);

        assertEquals(-1, cpu.r[0], "checking an id this single-slot model never issued must report GDC_STATUS_ERROR");
    }

    @Test
    void gdromUnknownFunctionReportsFailureHonestly() {
        MemBus bus = new MemBus();
        BiosSyscallHandler.installVectorTable(bus);
        BiosSyscallHandler handler = new BiosSyscallHandler(bus);
        Sh4Cpu cpu = cpuAt(bus, bus.read32(BiosSyscallHandler.VECTOR_MISC_GDROM));
        cpu.r[6] = 0;  // GDROM superfunction
        cpu.r[7] = 6;  // GDROM_REQ_DMA — real function, but not implemented in this pass

        handler.handle(cpu);

        assertEquals(-1, cpu.r[0]);
    }

    @Test
    void handleThrowsForAnAddressThatIsNotATrap() {
        MemBus bus = new MemBus();
        BiosSyscallHandler handler = new BiosSyscallHandler(bus);
        Sh4Cpu cpu = cpuAt(bus, 0x8C010000);

        assertThrows(IllegalArgumentException.class, () -> handler.handle(cpu));
    }
}
