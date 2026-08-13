package org.dreamjemu.gpu.pvr2;

import org.dreamjemu.system.MemoryRegion;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holly's "System Control Reg." block — physical range {@code 0x005F6800}-{@code 0x005F69FF} /
 * P2 (non-cacheable) mirror {@code 0xA05F6800}-{@code 0xA05F69FF}, {@code SIZE} bytes. Distinct
 * from {@link PvrRegisters}' "TA/PVR Core Reg." block ({@code 0x005F8000}-{@code 0x005F9FFF}) —
 * confirmed as two separate named regions by the official Sega Dev.Box System Architecture
 * manual's own physical memory map (Table 2-1). Lives in this package for now alongside
 * {@link PvrRegisters} since the one register modeled here is graphics-adjacent (see below), even
 * though officially it belongs to Holly's general system/interrupt-controller block, not PVR2
 * core — a future dedicated "system bus" module would be the more precise home if one is ever
 * added.
 *
 * <p>Added specifically because a real Sonic Adventure run reached a second budget-exhausting
 * spin-wait immediately after the one {@link PvrRegisters}' {@code SPG_STATUS} fixed (same real
 * opcode shape: {@code MOV.L @R5,R2} / {@code TST R4,R2} / {@code BT -4}, with
 * {@code R5=0xA05F6900}, {@code R4=0x8}). Confirmed against two independent authoritative
 * sources that agree exactly: the official Sega Dev.Box System Architecture manual (which lists
 * {@code 0x005F6900 SB_ISTNRM RW "Normal interrupt status"}), and KallistiOS's own {@code asic.h}
 * (whose {@code ASIC_EVT_PVR_VBLANK_BEGIN = 0x0003} — KOS's ASIC event codes {@code 0x0000}-
 * {@code 0x00FF} map directly to {@code SB_ISTNRM} bit positions, so this confirms bit 3 of
 * {@code SB_ISTNRM} is "VBLANK begin interrupt"). The real loop was masking with {@code 0x8}
 * (bit 3) and waiting for it to become nonzero — genuine VBlank-wait code, a completely coherent
 * continuation of the {@code SPG_STATUS} scanline poll it followed.
 *
 * <p><b>Only {@code SB_ISTNRM} is modeled, and only its {@code VBLANK_BEGIN} bit (3) — set by
 * real, autonomous video timing, not by software reading this register.</b> An earlier version
 * of this class made {@code VBLANK_BEGIN} always read as pending, which — on reflection — has
 * the same problem {@code PvrRegisters}' old read-driven {@code SPG_STATUS} had: it makes the
 * *symptom* (this one poll loop) disappear without the register meaning anything close to what
 * real hardware's interrupt-status register means. This version instead only ever sets
 * {@code VBLANK_BEGIN} when {@link #setVblankBeginPending()} is called — which {@code app-cli}'s
 * {@code Main} does exactly when {@link PvrRegisters#tick()} reports a real (approximated, but
 * autonomous) VBlank actually beginning, and — crucially — {@code Main} also now tries to
 * deliver a genuine SH-4 interrupt at that same moment ({@code Sh4Cpu.tryDeliverInterrupt}), so
 * code that's genuinely waiting for a real interrupt (not just polling this register) gets one
 * for real. Reads simply return the real current value — no synthesis, no side effect. Writes
 * honor real hardware's documented write-1-to-clear acknowledgement convention
 * ({@link #writeIstnrm(int)}), so real code that acks the interrupt behaves exactly like it
 * would on real hardware: the bit stays clear until the next genuine VBlank actually happens (not
 * "on the next read", the way the old version faked it).
 *
 * <p>Every other register in this block falls back to exactly
 * {@link org.dreamjemu.system.UnmappedRegion}'s behavior (log at {@code FINE}, read as
 * {@code 0}, ignore writes) until a real disc run confirms one is actually needed — this
 * project's standing "implement only what a real disc image actually needs" discipline.
 */
public final class HollySystemRegisters implements MemoryRegion {

    private static final Logger LOG = Logger.getLogger(HollySystemRegisters.class.getName());

    /**
     * Size of the real "System Control Reg." block, confirmed against the official Sega
     * Dev.Box System Architecture manual's own physical memory map (Table 2-1): {@code 0x200}
     * bytes ({@code 0x005F6800}-{@code 0x005F69FF} inclusive).
     */
    public static final long SIZE = 0x200L;

    /**
     * Offset of {@code SB_ISTNRM} within this block ({@code 0x005F6900 - 0x005F6800}),
     * confirmed against the same manual, which lists it explicitly at that absolute address.
     */
    private static final long SB_ISTNRM_OFFSET = 0x100L;

    /**
     * Bit 3 of {@code SB_ISTNRM} — "VBLANK begin interrupt". Confirmed against KallistiOS's own
     * {@code asic.h}: {@code ASIC_EVT_PVR_VBLANK_BEGIN = 0x0003} — see this class's Javadoc for
     * why KOS's event code doubles as the real bit position here.
     */
    public static final int VBLANK_BEGIN_BIT = 1 << 3;

    /**
     * Real SH-4 interrupt priority level for Holly's "normal" interrupt group (which
     * {@code SB_ISTNRM} — and so {@code VBLANK_BEGIN} — belongs to), confirmed against two
     * independent authoritative sources that agree exactly: lxdream's own {@code intc.c}
     * interrupt-code table (listing {@code IRQ9 = 0x320} for this group), and the Linux kernel's
     * {@code mach-dreamcast} IRQ demux source, which groups Holly's three interrupt status
     * registers ({@code ISTNRM}/{@code ISTEXT}/{@code ISTERR}) into exactly three SH-4 external
     * interrupt levels (9/11/13) in that same order.
     */
    public static final int NORMAL_INTERRUPT_PRIORITY_LEVEL = 9;

    /**
     * Real {@code INTEVT} code for Holly's "normal" interrupt group, confirmed against the same
     * lxdream {@code intc.c} table (the entry paired with {@code IRQ9}, above).
     */
    public static final int NORMAL_INTERRUPT_INTEVT = 0x320;

    private int istnrm;

    @Override
    public String name() {
        return "Holly system control registers (core-gpu-pvr2)";
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
        if (offset == SB_ISTNRM_OFFSET) {
            // A plain read of real state — no side effect on the value itself, unlike the
            // always-pending placeholder this replaced. See this class's Javadoc.
            return istnrm;
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
        if (offset == SB_ISTNRM_OFFSET) {
            writeIstnrm(value);
        } else {
            logAccess("write32", offset);
        }
    }

    @Override
    public void write64(long offset, long value) {
        logAccess("write64", offset);
    }

    /**
     * Whether any bit in this real "normal" interrupt-status register is currently pending —
     * i.e. whether real Holly hardware would be continuously asserting its normal-interrupt line
     * right now. Used by {@code app-cli}'s {@code Main} to retry interrupt delivery every step
     * (not just the one step {@link #setVblankBeginPending()} was called on), exactly matching
     * how a real, continuously-asserted hardware interrupt line behaves — the CPU accepts it
     * whenever it's next unmasked, which might not be the same step it became pending.
     */
    public boolean hasPendingNormalInterrupt() {
        return istnrm != 0;
    }

    /**
     * Sets {@code VBLANK_BEGIN} for real — meant to be called by whoever owns both this class and
     * {@link PvrRegisters} (currently {@code app-cli}'s {@code Main}) exactly when
     * {@link PvrRegisters#tick()} reports a real VBlank beginning, not on every read. See this
     * class's Javadoc for the full reasoning.
     */
    public void setVblankBeginPending() {
        istnrm |= VBLANK_BEGIN_BIT;
    }

    /**
     * Real hardware's documented write-1-to-clear acknowledgement convention: writing a 1 to a
     * bit clears that bit (writing 0 to a bit leaves it unchanged). Unlike the placeholder this
     * replaced, an acked bit now stays clear until {@link #setVblankBeginPending()} is genuinely
     * called again at the next real VBlank — not "on the very next read" (see this class's
     * Javadoc).
     */
    private void writeIstnrm(int value) {
        istnrm &= ~value;
    }

    private void logAccess(String kind, long offset) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine(() -> String.format("%s: unimplemented %s at offset 0x%X", name(), kind, offset));
        }
    }
}
