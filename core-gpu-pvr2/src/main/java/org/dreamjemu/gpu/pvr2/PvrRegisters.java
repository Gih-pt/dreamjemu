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
 * <p><b>Only {@code SPG_STATUS} is modeled, and only approximately.</b> This project has no real
 * video timing yet (no pixel clock, no frame period, no interlace/field tracking) — implementing
 * that accurately is a much larger undertaking than this one real-world blocker calls for. What
 * {@link #readSpgStatus()} does instead is the minimum needed to unblock this specific confirmed
 * real-world poll: increment a free-running counter by 1 on every {@code SPG_STATUS} read, so
 * "wait until the scanline counter is nonzero" (the actual real-world loop found) terminates
 * after its first read here, instead of forever (this block previously fell into
 * {@link org.dreamjemu.system.UnmappedRegion}, which always returns {@code 0} — the loop's exact
 * failure mode). This is a deliberate placeholder, not real timing — {@code fieldnum}/
 * {@code blank}/{@code hsync}/{@code vsync} (bits 10-13) are left at {@code 0} since nothing has
 * confirmed a real need for them yet, and the counter advances once per *register read*, not
 * once per real scanline period. If/when real video timing is needed (frame-rate-accurate
 * rendering, VBlank-interrupt-driven code, etc.), this needs to be replaced with a real
 * pixel-clock-driven counter — this comment is the marker for that.
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

    /** The 10-bit {@code scanline} field's mask — see {@link #readSpgStatus()}'s Javadoc. */
    private static final int SCANLINE_MASK = 0x3FF;

    private int scanline;

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
            return readSpgStatus();
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
     * Synthesizes {@code SPG_STATUS} — see this class's Javadoc for why this is a deliberate,
     * read-driven placeholder rather than real pixel-clock timing. Wraps within the real
     * hardware's 10-bit {@code scanline} field ({@code fieldnum}/{@code blank}/{@code hsync}/
     * {@code vsync}, bits 10-13, are left {@code 0} — not modeled).
     */
    private int readSpgStatus() {
        scanline = (scanline + 1) & SCANLINE_MASK;
        return scanline;
    }

    private void logAccess(String kind, long offset) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine(() -> String.format("%s: unimplemented %s at offset 0x%X", name(), kind, offset));
        }
    }
}
