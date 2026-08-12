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
 * <p><b>Only {@code SB_ISTNRM} is modeled, and only its {@code VBLANK_BEGIN} bit (3), and only
 * approximately</b> — same reasoning as {@link PvrRegisters}' {@code SPG_STATUS}: this project
 * has no real video timing yet, so a real VBlank interrupt can never actually fire. Rather than
 * build a real interrupt controller for this one confirmed blocker, {@link #readIstnrm()} always
 * reports {@code VBLANK_BEGIN} as pending on every read — enough for "wait until VBlank begins"
 * to terminate immediately, every time it's polled, instead of never (this address previously
 * fell into {@link org.dreamjemu.system.UnmappedRegion}, which always returns {@code 0} — the
 * loop's exact failure mode). Writes are honored as the real hardware's documented
 * write-1-to-clear acknowledgement convention ({@link #writeIstnrm(int)}), so real code that acks
 * the interrupt behaves sensibly — the bit simply reappears on the next read, since nothing here
 * tracks real frame timing to know when a *genuine* next VBlank would occur. This is a deliberate
 * placeholder, not a real interrupt controller — the marker for where real Holly interrupt
 * timing (and the other {@code SB_ISTNRM}/{@code SB_ISTEXT}/{@code SB_ISTERR} bits — GD-ROM DMA
 * done, Maple DMA done, PVR render done, etc.) would need to replace this, if/when a real disc
 * run confirms one of those is actually needed.
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
    private static final int VBLANK_BEGIN_BIT = 1 << 3;

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
            return readIstnrm();
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
     * Synthesizes {@code SB_ISTNRM} — see this class's Javadoc for why {@code VBLANK_BEGIN} is
     * always reported pending, rather than driven by real frame timing.
     */
    private int readIstnrm() {
        istnrm |= VBLANK_BEGIN_BIT;
        return istnrm;
    }

    /**
     * Real hardware's documented write-1-to-clear acknowledgement convention: writing a 1 to a
     * bit clears that bit (writing 0 to a bit leaves it unchanged). {@link #readIstnrm()} will
     * simply set {@code VBLANK_BEGIN} again on the next read regardless — see this class's
     * Javadoc.
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
