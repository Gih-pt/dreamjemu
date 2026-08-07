package org.dreamjemu.cli;

import org.dreamjemu.common.log.Logger;
import org.dreamjemu.cpu.sh4.Sh4Cpu;
import org.dreamjemu.system.Bus;

/**
 * Intercepts and handles the Dreamcast's documented BIOS system calls, instead of trying to
 * interpret real BIOS machine code this project doesn't have.
 *
 * <p><b>Why this exists:</b> real commercial games rely on these syscalls constantly (GD-ROM
 * reads especially) — a real BIOS installs a small vector table in RAM before jumping to the
 * game, and code calls through it (see the class-level "calling convention" note below). This
 * project's BIOS-free HLE boot ({@code HleBootLoader}) never set this table up at all, so any
 * code trying to use it would read zero and either crash immediately or (as this class was
 * written to investigate) unwind unpredictably. This does <b>not</b>, on its own, explain or fix
 * the specific {@code PC=0} boundary found via a real Sonic Adventure run on 2026-08-04 (that was
 * a genuine {@code RTS} to an uninitialized {@code PR}, not a syscall-vector jump — see
 * {@code Main}'s {@code PC=0} diagnostic message) — this is separate, forward-looking groundwork
 * for whatever comes after that boundary is understood or worked around.
 *
 * <p><b>Calling convention</b> (confirmed against Marcus Comstedt's authoritative "Dreamcast
 * Programming - System Calls" page, {@code mc.pp.se/dc/syscalls.html} — the same source already
 * used for {@code core-maple}'s protocol work): each syscall is reached through an indirect
 * vector — code reads a function pointer from one of 4 fixed RAM addresses and calls through it,
 * rather than calling a fixed address directly. The function within that vector's group is
 * selected by {@code r7} (superfunction, where applicable, by {@code r6}); arguments go in
 * {@code r4}/{@code r5} (and {@code r6}, when not used for superfunction selection); the result
 * goes in {@code r0}, the same as a normal SH-4 function call. {@code ROMFONT} is documented as
 * an explicit exception: it uses {@code r1} instead of {@code r7} to select its function.
 *
 * <p><b>Scope, honestly stated:</b> only the state-free syscalls (ones whose entire documented
 * behavior doesn't depend on data this project doesn't have — flashrom contents, ROM font
 * glyphs, a real hardware ID, or actual asynchronous GD-ROM command processing) return their
 * real documented success value. Everything else returns the documented failure value and logs
 * clearly why, rather than fabricating data or silently pretending to succeed — the same "gaps
 * are loud" discipline {@code Sh4Cpu} already follows for unimplemented opcodes and
 * {@code UnmappedRegion} follows for unmapped memory. Real GD-ROM reads already exist in this
 * project (see {@code core-gdrom}'s {@code SectorSource}/{@code Iso9660FileSystem}) — wiring
 * {@code GDROM_SEND_COMMAND} et al. to actually use them is deliberately left for later, not
 * attempted in this first pass, to keep this change reviewable.
 */
public final class BiosSyscallHandler {

    private static final Logger LOG = Logger.get(BiosSyscallHandler.class);

    // Vector addresses — fixed RAM locations real code reads a function pointer from. Confirmed
    // against mc.pp.se/dc/syscalls.html.
    public static final int VECTOR_SYSINFO = 0x8C0000B0;
    public static final int VECTOR_ROMFONT = 0x8C0000B4;
    public static final int VECTOR_FLASHROM = 0x8C0000B8;
    public static final int VECTOR_MISC_GDROM = 0x8C0000BC;

    // This project's OWN trap addresses — NOT real BIOS syscall entry-point addresses (this
    // project has no real BIOS binary to place real entry code at). Chosen within the documented
    // "resides in the RAM area 8C000000-8C007FFF" syscall region (mc.pp.se, same page) so they
    // read as plausible to anything inspecting the vector table, but their actual "handler" is
    // this Java class — see isSyscallTrap()/handle() below — not real machine code sitting there.
    private static final int TRAP_SYSINFO = 0x8C000100;
    private static final int TRAP_ROMFONT = 0x8C000104;
    private static final int TRAP_FLASHROM = 0x8C000108;
    private static final int TRAP_MISC_GDROM = 0x8C00010C;

    private BiosSyscallHandler() {
    }

    /**
     * Writes this class's trap addresses into the 4 documented vector locations — the part of
     * real BIOS boot behavior this project's BIOS-free HLE boot ({@code HleBootLoader}) never
     * performed. Call once, after loading the boot file and before starting the SH-4.
     */
    public static void installVectorTable(Bus bus) {
        bus.write32(Integer.toUnsignedLong(VECTOR_SYSINFO), TRAP_SYSINFO);
        bus.write32(Integer.toUnsignedLong(VECTOR_ROMFONT), TRAP_ROMFONT);
        bus.write32(Integer.toUnsignedLong(VECTOR_FLASHROM), TRAP_FLASHROM);
        bus.write32(Integer.toUnsignedLong(VECTOR_MISC_GDROM), TRAP_MISC_GDROM);
    }

    /** True if {@code pc} is one of this class's trap addresses — call this before executing whatever's "at" pc. */
    public static boolean isSyscallTrap(int pc) {
        return pc == TRAP_SYSINFO || pc == TRAP_ROMFONT || pc == TRAP_FLASHROM || pc == TRAP_MISC_GDROM;
    }

    /**
     * Handles the syscall at {@code cpu.pc} (must satisfy {@link #isSyscallTrap}), leaves a
     * result in {@code r[0]} per the syscall's documented convention, and sets
     * {@code cpu.pc = cpu.pr} — simulating the "return to caller" a real syscall handler's own
     * {@code RTS} would perform, without needing any actual machine code at the trap address.
     */
    public static void handle(Sh4Cpu cpu) {
        int pc = cpu.pc;
        if (pc == TRAP_SYSINFO) {
            handleSysinfo(cpu);
        } else if (pc == TRAP_ROMFONT) {
            handleRomfont(cpu);
        } else if (pc == TRAP_FLASHROM) {
            handleFlashrom(cpu);
        } else if (pc == TRAP_MISC_GDROM) {
            handleMiscGdrom(cpu);
        } else {
            throw new IllegalArgumentException("Not a known syscall trap address: 0x" + Integer.toHexString(pc));
        }
        cpu.pc = cpu.pr;
    }

    private static void handleSysinfo(Sh4Cpu cpu) {
        int fn = cpu.r[7];
        switch (fn) {
            case 0: // SYSINFO_INIT — real behavior: copies flashrom data to a fixed RAM address.
                     // We have no flashrom, but every real caller must call this before the other
                     // two anyway; reporting success (matching the documented return value) lets
                     // callers proceed to SYSINFO_ICON/SYSINFO_ID, which report their own honest
                     // failure below.
                LOG.info("SYSINFO_INIT");
                cpu.r[0] = 0;
                break;
            case 2: // SYSINFO_ICON
                LOG.warn("SYSINFO_ICON: not supported (no flashrom icon data) - returning failure");
                cpu.r[0] = -1;
                break;
            case 3: // SYSINFO_ID
                LOG.warn("SYSINFO_ID: not supported (no real hardware ID) - returning null pointer");
                cpu.r[0] = 0;
                break;
            default:
                LOG.warn("SYSINFO: unknown function r7=%d - returning failure", fn);
                cpu.r[0] = -1;
        }
    }

    private static void handleRomfont(Sh4Cpu cpu) {
        int fn = cpu.r[1]; // ROMFONT is the documented exception: r1 selects the function, not r7.
        switch (fn) {
            case 0: // ROMFONT_ADDRESS
                LOG.warn("ROMFONT_ADDRESS: not supported (no font ROM data) - returning null pointer");
                cpu.r[0] = 0;
                break;
            case 1: // ROMFONT_LOCK — nothing else in this interpreter contends for the font, so
                     // granting the mutex unconditionally matches real behavior for a single caller.
                LOG.info("ROMFONT_LOCK");
                cpu.r[0] = 0;
                break;
            case 2: // ROMFONT_UNLOCK — documented as having no return value.
                LOG.info("ROMFONT_UNLOCK");
                break;
            default:
                LOG.warn("ROMFONT: unknown function r1=%d - returning failure", fn);
                cpu.r[0] = -1;
        }
    }

    private static final String[] FLASHROM_FUNCTION_NAMES = {
            "FLASHROM_INFO", "FLASHROM_READ", "FLASHROM_WRITE", "FLASHROM_DELETE"
    };

    private static void handleFlashrom(Sh4Cpu cpu) {
        int fn = cpu.r[7];
        String name = (fn >= 0 && fn < FLASHROM_FUNCTION_NAMES.length) ? FLASHROM_FUNCTION_NAMES[fn] : null;
        if (name != null) {
            LOG.warn("%s: not supported (no flashrom emulation) - returning failure", name);
        } else {
            LOG.warn("FLASHROM: unknown function r7=%d - returning failure", fn);
        }
        cpu.r[0] = -1;
    }

    private static void handleMiscGdrom(Sh4Cpu cpu) {
        int superFn = cpu.r[6]; // -1 = MISC, 0 = GDROM, 1-7 = user-defined (never installed here).
        int fn = cpu.r[7];
        if (superFn == -1) {
            switch (fn) {
                case 0: // MISC_INIT — real behavior re-initializes all vectors to default. This
                         // project's vectors are already installed by installVectorTable(), so
                         // reporting success (matching the documented return value) is accurate.
                    LOG.info("MISC_INIT");
                    cpu.r[0] = 0;
                    break;
                case 1: // MISC_SETVECTOR — would let the game install its own superfunction
                         // handler; not implemented yet, honestly reported as such.
                    LOG.warn("MISC_SETVECTOR: not supported yet (custom syscall vectors) - returning failure");
                    cpu.r[0] = -1;
                    break;
                default:
                    LOG.warn("MISC: unknown function r7=%d - returning failure", fn);
                    cpu.r[0] = -1;
            }
        } else if (superFn == 0) {
            LOG.warn("GDROM_* (r7=%d): not implemented yet - returning failure. Real GD-ROM access "
                    + "already exists via core-gdrom's SectorSource/Iso9660FileSystem, just not "
                    + "wired into these syscalls yet.", fn);
            cpu.r[0] = -1;
        } else {
            LOG.warn("MISC/GDROM: unknown superfunction r6=%d, function r7=%d - returning failure", superFn, fn);
            cpu.r[0] = -1;
        }
    }
}
