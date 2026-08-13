package org.dreamjemu.gpu.pvr2;

import org.dreamjemu.system.MemoryRegion;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PVR2 (Holly) register block — physical range {@code 0x005F8000}-{@code 0x005F87FF} /
 * P2 (non-cacheable) mirror {@code 0xA05F8000}-{@code 0xA05F87FF}, {@code SIZE} bytes.
 *
 * <p>Added specifically because a real Sonic Adventure run reached a stable 3-instruction loop
 * (real opcode: {@code MOV.L @R4,R3} / {@code TST R5,R3} / {@code BT -4}, with
 * {@code R4=0xA05F810C}) that ran for the entire 100,000,000-step budget without ever leaving
 * it — a classic "poll a hardware status register until a bit sets" spin. Confirmed against two
 * independent authoritative sources that agree exactly (KallistiOS's own {@code pvr_regs.h}, and
 * the Flycast emulator's {@code pvr_regs.h}): {@code 0x005F810C} (offset {@code 0x10C} from this
 * block's base) is {@code SPG_STATUS} — "Sync pulse generator status", a read-only register
 * whose low 10 bits report the video hardware's current scanline number, counting up once per
 * frame as the display is drawn.
 *
 * <p><b>Only {@code SPG_STATUS} is modeled, and only approximately — but driven by real,
 * autonomous elapsed time, not by software polling it.</b> An earlier version of this class made
 * the scanline counter advance on every <i>read</i>, which — on reflection — is backwards: real
 * video hardware runs continuously whether or not software ever looks at it; a register that only
 * changes because software happened to poll it isn't modeling the hardware, it's just making the
 * specific poll loop that was found go away. {@link #tick()} fixes this: it's driven by
 * {@code app-cli}'s own step loop (once per {@code Sh4Cpu.step()}, regardless of whether anything
 * reads {@code SPG_STATUS} at all), so the scanline counter — and, in turn, the VBlank event
 * {@link #tick()} reports — genuinely reflects elapsed emulated program execution, not read
 * frequency.
 *
 * <p>This project has no real per-instruction cycle accounting yet (each {@code Sh4Cpu.step()}
 * is treated as elapsing an equal, undifferentiated unit of time, regardless of which real SH-4
 * instruction it executed), so the one deliberate, clearly-labeled approximation left is
 * {@code 1 step ≈ 1 real SH-4 clock cycle}. Every other constant this timing is derived from is
 * real, cited hardware: {@link #SH4_CLOCK_HZ} (the documented SH7091/SH-4 200MHz core clock) and
 * {@link #NTSC_FIELD_RATE_HZ}/{@link #LINES_PER_FIELD} (the standard NTSC field rate and
 * 262-line field length). {@link #STEPS_PER_LINE} is derived from those, not invented, and
 * VBlank is reported as beginning exactly when the per-field line counter wraps back to line 0 —
 * a reasonable, transparent approximation of "start of vertical blanking" given this project
 * doesn't parse the real {@code SPG_LOAD}/{@code SPG_VBLANK} configuration registers real code
 * would otherwise use to define the exact active/blanking split (those aren't implemented yet —
 * nothing has confirmed a need for them beyond this approximation so far). If/when real
 * per-instruction cycle costs or real {@code SPG_LOAD}-driven timing are needed, this is the
 * marker for where to replace the approximation, not the "1 step ≈ 1 cycle" premise most bring-up
 * emulators start from before a cycle-accurate core exists.
 *
 * <p>Every other PVR2 register in this block (there are dozens — ID, REVISION, SOFTRESET,
 * STARTRENDER, the whole tile-accelerator register set, etc.) falls back to exactly
 * {@link org.dreamjemu.system.UnmappedRegion}'s behavior (log at {@code FINE}, read as
 * {@code 0}, ignore writes) until a real disc run confirms one is actually needed — this
 * project's standing "implement only what a real disc image actually needs" discipline, applied
 * to a register block instead of a CPU opcode this time.
 */
public final class PvrRegisters implements MemoryRegion {

    private static final Logger LOG = Logger.getLogger(PvrRegisters.class.getName());

    /**
     * Size of the real PVR2 register block, confirmed against two independent authoritative
     * sources that agree exactly (KallistiOS's {@code pvr_regs.h} region-mask constant and
     * Flycast's {@code pvr_RegSize}/{@code pvr_RegMask}, both {@code 0x8000}).
     */
    public static final long SIZE = 0x8000L;

    /**
     * Offset of {@code SPG_STATUS} within this block, confirmed against the same two sources
     * (both list {@code SPG_STATUS_addr}/{@code 0x0000010C} identically) — see this class's
     * Javadoc for the full real-world context this was found in.
     */
    private static final long SPG_STATUS_OFFSET = 0x10CL;

    /** SH7091/SH-4's real, documented core clock frequency. */
    private static final long SH4_CLOCK_HZ = 200_000_000L;

    /** Standard NTSC field rate. */
    private static final double NTSC_FIELD_RATE_HZ = 59.94;

    /**
     * Standard NTSC field length. See this class's Javadoc for why this is used as the wrap
     * point for reporting a VBlank, even though real hardware's active/blanking split within a
     * field is configured by {@code SPG_LOAD}/{@code SPG_VBLANK} (not parsed here yet).
     */
    private static final int LINES_PER_FIELD = 262;

    /**
     * Derived, not invented — see this class's Javadoc for the one approximation this rests on
     * ({@code 1 step ≈ 1 cycle}). {@code SH4_CLOCK_HZ / NTSC_FIELD_RATE_HZ / LINES_PER_FIELD}.
     */
    private static final long STEPS_PER_LINE =
            Math.round(SH4_CLOCK_HZ / NTSC_FIELD_RATE_HZ / LINES_PER_FIELD);

    private int scanline;
    private long stepsSinceLastLine;

    @Override
    public String name() {
        return "PVR2 registers (core-gpu-pvr2)";
    }

    @Override
    public long size() {
        return SIZE;
    }

    @Override
    public byte read8(long offset) {
        logAccess("read8", offset);
        return 0;
    }

    @Override
    public short read16(long offset) {
        logAccess("read16", offset);
        return 0;
    }

    @Override
    public int read32(long offset) {
        if (offset == SPG_STATUS_OFFSET) {
            // A plain read of real, tick-driven state — no side effect on the value itself,
            // unlike the read-driven placeholder this replaced. See this class's Javadoc.
            return scanline;
        }
        logAccess("read32", offset);
        return 0;
    }

    @Override
    public long read64(long offset) {
        logAccess("read64", offset);
        return 0;
    }

    @Override
    public void write8(long offset, byte value) {
        logAccess("write8", offset);
    }

    @Override
    public void write16(long offset, short value) {
        logAccess("write16", offset);
    }

    @Override
    public void write32(long offset, int value) {
        logAccess("write32", offset);
    }

    @Override
    public void write64(long offset, long value) {
        logAccess("write64", offset);
    }

    /**
     * Advances real, autonomous video timing by one emulated step — see this class's Javadoc for
     * the full reasoning. Meant to be called exactly once per {@code Sh4Cpu.step()} by whoever
     * owns both (currently {@code app-cli}'s {@code Main}), regardless of whether anything reads
     * {@code SPG_STATUS} in between.
     *
     * @return {@code true} exactly on the step where the per-field line counter wraps back to
     *         line 0 — the approximation this class uses for "a new field's VBlank has begun"
     *         (see this class's Javadoc). Callers are expected to react to this (set
     *         {@code HollySystemRegisters}' {@code VBLANK_BEGIN} bit and try to deliver a real
     *         CPU interrupt) — {@code false} every other step.
     */
    public boolean tick() {
        stepsSinceLastLine++;
        if (stepsSinceLastLine < STEPS_PER_LINE) {
            return false;
        }
        stepsSinceLastLine = 0;
        scanline++;
        if (scanline >= LINES_PER_FIELD) {
            scanline = 0;
            return true;
        }
        return false;
    }

    private void logAccess(String kind, long offset) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine(() -> String.format("%s: unimplemented %s at offset 0x%X", name(), kind, offset));
        }
    }
}
