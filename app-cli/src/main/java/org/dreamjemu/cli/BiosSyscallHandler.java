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
 * <p><b>Scope, honestly stated:</b> the state-free syscalls (ones whose entire documented
 * behavior doesn't depend on data this project doesn't have — flashrom contents, ROM font
 * glyphs, a real hardware ID) return their real documented success value. The GDROM command
 * queue's own mechanics (assigning request IDs, tracking a command's status/error, {@code
 * GDROM_INIT}/{@code ABORT_COMMAND}/{@code RESET} housekeeping — see {@link #handleGdrom}) are
 * implemented for real, and control-only GD-ROM commands (ones with no real disc-data dependency,
 * e.g. {@code GDC_SEEK}/{@code GDC_STOP}) report real success too. Everything else — flashrom
 * writes, ROM font data, and GD-ROM commands that would need real sector data, a TOC, or CDDA
 * playback this project doesn't have wired up yet — returns the documented failure value and
 * logs clearly why, rather than fabricating data or silently pretending to succeed — the same
 * "gaps are loud" discipline {@code Sh4Cpu} already follows for unimplemented opcodes and
 * {@code UnmappedRegion} follows for unmapped memory. Real GD-ROM sector reads already exist in
 * this project (see {@code core-gdrom}'s {@code SectorSource}/{@code Iso9660FileSystem}) —
 * wiring {@code GDC_PIOREAD}/{@code GDC_DMAREAD} to actually use them needs a disc-absolute FAD
 * to track-relative-LBA conversion this project doesn't have a confirmed offset for yet; see
 * {@link #handleGdrom}'s own Javadoc for why that's deliberately left for a future pass rather
 * than guessed at here.
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

    // GDROM_* function codes (selected by r7 when r6 == GDROM_SUPERFUNCTION, i.e. 0) — confirmed
    // against mc.pp.se/dc/syscalls.html's own prose description of each function, cross-checked
    // against redream's syscalls.c (github.com/inolen/redream, a real, independently-tested
    // emulator implementation of this exact interface) for the numeric values themselves, which
    // mc.pp.se's page describes but doesn't tabulate as plainly.
    private static final int GDROM_SUPERFUNCTION = 0;
    private static final int GDROM_SEND_COMMAND = 0x0;
    private static final int GDROM_CHECK_COMMAND = 0x1;
    private static final int GDROM_MAINLOOP = 0x2; // aka "exec server" in KallistiOS's own naming
    private static final int GDROM_INIT = 0x3;
    private static final int GDROM_ABORT_COMMAND = 0x8;
    private static final int GDROM_RESET = 0x9;

    // GDC_* command codes — the values SEND_COMMAND's r4 argument selects between, i.e. what's
    // actually being enqueued (as opposed to the GDROM_* functions above, which are how you talk
    // to the queue itself). Same sources as GDROM_* above.
    private static final int GDC_PIOREAD = 0x10;
    private static final int GDC_DMAREAD = 0x11;
    private static final int GDC_GETTOC = 0x12;
    private static final int GDC_GETTOC2 = 0x13;
    private static final int GDC_PLAY = 0x14;
    private static final int GDC_PLAY2 = 0x15;
    private static final int GDC_PAUSE = 0x16;
    private static final int GDC_RELEASE = 0x17;
    private static final int GDC_INIT = 0x18;
    private static final int GDC_SEEK = 0x1b;
    private static final int GDC_READ = 0x1c;
    private static final int GDC_REQ_MODE = 0x1e;
    private static final int GDC_SET_MODE = 0x1f;
    private static final int GDC_STOP = 0x21;
    private static final int GDC_GET_SCD = 0x22;
    private static final int GDC_REQ_SES = 0x23;
    private static final int GDC_REQ_STAT = 0x24;
    private static final int GDC_GET_VER = 0x28;

    // GDC_STATUS_* — same sources as above. A command's status as reported by CHECK_COMMAND.
    private static final int GDC_STATUS_ERROR = -1;
    private static final int GDC_STATUS_INACTIVE = 0x0;
    private static final int GDC_STATUS_COMPLETE = 0x2;

    // GDC_ERROR_* — the generic error code CHECK_COMMAND writes to the first of its 4
    // extended-status ints (mc.pp.se: "The first is a generic error code").
    private static final int GDC_ERROR_OK = 0x0;
    private static final int GDC_ERROR_SYSTEM = 0x1;
    private static final int GDC_ERROR_INVALID_CMD = 0x5;

    // FLASHROM_* partition table — confirmed against mc.pp.se/dc/syscalls.html's own worked
    // example table (the real, physical 128KB flashrom chip's 5 fixed partitions). Index is the
    // partition number FLASHROM_INFO's r4 selects; FLASHROM_PARTITION_OFFSETS/SIZES give each
    // partition's absolute offset/size within the chip.
    private static final int[] FLASHROM_PARTITION_OFFSETS = {0x1A000, 0x18000, 0x1C000, 0x10000, 0x00000};
    private static final int[] FLASHROM_PARTITION_SIZES = {0x2000, 0x2000, 0x4000, 0x8000, 0x10000};
    private static final int FLASHROM_SIZE = 0x20000; // 128KB total — confirmed by the same table.

    /**
     * A real, in-memory-backed emulation of the physical 128KB flashrom chip — not a
     * per-call fabrication. Initialized to {@code 0xFF} throughout (see the constructor):
     * real NOR flash's documented erased-cell value (confirmed against both mc.pp.se's own
     * description of the erase operation and redream's independent {@code flash.c}, whose
     * {@code flash_erase} explicitly fills with {@code 0xff}) — matching a factory-fresh,
     * never-configured console exactly, which is the only honest state this project can claim
     * without a real dumped {@code flash.bin} (this project has none, and fabricating specific
     * "realistic" region/language bytes without a confirmed source would be exactly the kind of
     * unlabeled guess this project's discipline avoids). {@link #handleFlashrom} reads/writes
     * real bytes here — {@code FLASHROM_WRITE} only ever clears bits ({@code &=}, never sets),
     * the real physical constraint of NOR flash without a prior erase, also confirmed against
     * both of those same sources.
     */
    private final byte[] flashRom = new byte[FLASHROM_SIZE];

    /**
     * This project's own single-slot model of the real GDROM command queue: SEND_COMMAND
     * determines a command's final outcome immediately (synchronously — this interpreter has no
     * asynchronous GD-ROM controller to actually wait on), and CHECK_COMMAND reports that stored
     * outcome back later. Real hardware supports queuing more than one command and genuinely
     * asynchronous completion (which is why GDROM_MAINLOOP/"exec server" needs to be polled at
     * all); this project's single slot is an honest simplification given every command here
     * completes (or fails) the instant it's sent, not a claim that real hardware only queues one
     * command at a time.
     */
    private int nextRequestId = 1;
    private int slotRequestId = 0; // 0 = no command has ever been sent yet
    private int slotStatus = GDC_STATUS_INACTIVE;
    private int slotError = GDC_ERROR_OK;

    /**
     * The fixed RAM address {@code SYSINFO_INIT} copies flashrom data to, and {@code
     * SYSINFO_ID}/{@code SYSINFO_ICON} read back from — confirmed against mc.pp.se/dc/
     * syscalls.html, which documents the copied range as {@code 8C000068}-{@code 8C00007F}
     * (24 bytes) and separately documents {@code SYSINFO_ID} as returning "a pointer to where
     * the ID is stored as 8 contiguous bytes", which every independent real-world reference
     * (KallistiOS, redream) places at the very start of that same copied range.
     */
    private static final int SYSINFO_DATA_ADDRESS = 0x8C000068;
    private static final int SYSINFO_DATA_LENGTH = 0x18; // 24 bytes — 8C000068 through 8C00007F inclusive.

    private final Bus bus;

    /**
     * @param bus the same {@link Bus} the emulated machine's {@code Sh4Cpu} runs against — needed
     *            to write {@code GDROM_CHECK_COMMAND}'s 4-int extended-status block back into
     *            guest RAM at the address the guest itself provided (see {@link
     *            #handleGdrom}), and similarly for {@link #handleFlashrom}/{@link
     *            #handleSysinfo}'s own real memory movement; {@code Sh4Cpu} deliberately keeps
     *            its own {@code Bus} reference private (see its Javadoc), so this is passed in
     *            separately, the same way {@link #installVectorTable} already takes a
     *            {@code Bus} rather than a {@code Sh4Cpu}.
     */
    public BiosSyscallHandler(Bus bus) {
        this.bus = bus;
        java.util.Arrays.fill(flashRom, (byte) 0xFF);
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
     *
     * <p>Instance method (not {@code static}, unlike this class's other methods): the GDROM
     * command-queue functions below need state that persists across calls (see {@link
     * #slotStatus}'s Javadoc) — one {@code BiosSyscallHandler} instance per emulated machine,
     * the same way one {@code Sh4Cpu} instance is used per machine.
     */
    public void handle(Sh4Cpu cpu) {
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


    private void handleSysinfo(Sh4Cpu cpu) {
        int fn = cpu.r[7];
        switch (fn) {
            case 0: // SYSINFO_INIT — real behavior: copies flashrom data to a fixed RAM address
                     // (see SYSINFO_DATA_ADDRESS's own Javadoc for the confirmed source/target).
                     // Copies real bytes from this project's own emulated flashrom (see
                     // flashRom's own Javadoc for why those bytes are honestly all 0xFF, not
                     // fabricated "realistic" values) — a factory-fresh console's real state,
                     // not a fabrication of what SYSINFO_ID/SYSINFO_ICON would then report.
                LOG.info("SYSINFO_INIT");
                for (int i = 0; i < SYSINFO_DATA_LENGTH; i++) {
                    bus.write8(Integer.toUnsignedLong(SYSINFO_DATA_ADDRESS + i),
                            flashRom[FLASHROM_PARTITION_OFFSETS[0] + i]);
                }
                cpu.r[0] = 0;
                break;
            case 2: // SYSINFO_ICON
                LOG.warn("SYSINFO_ICON: not supported - the real icon data format is genuinely "
                        + "undocumented (mc.pp.se's own page: \"The format those icons are in is "
                        + "not known\") - returning failure rather than fabricating a format "
                        + "nobody has confirmed");
                cpu.r[0] = -1;
                break;
            case 3: // SYSINFO_ID — real hardware documents no failure return value at all; always
                     // returns a pointer to the 8 bytes SYSINFO_INIT copied above (see
                     // SYSINFO_DATA_ADDRESS's own Javadoc). Returning a real, stable, non-null
                     // pointer (even to this project's honestly-blank flashrom data) matches real
                     // behavior far more closely than the null pointer this used to return, which
                     // a real caller — since real hardware never needs to check for one — might
                     // never even guard against.
                LOG.info("SYSINFO_ID");
                cpu.r[0] = SYSINFO_DATA_ADDRESS;
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

    private void handleFlashrom(Sh4Cpu cpu) {
        int fn = cpu.r[7];
        switch (fn) {
            case 0: { // FLASHROM_INFO — r4=partition, r5=ptr to two int32s (offset, size).
                int partition = cpu.r[4];
                if (partition < 0 || partition >= FLASHROM_PARTITION_OFFSETS.length) {
                    LOG.warn("FLASHROM_INFO: no such partition %d - returning failure", partition);
                    cpu.r[0] = -1;
                    break;
                }
                long ptr = Integer.toUnsignedLong(cpu.r[5]);
                bus.write32(ptr, FLASHROM_PARTITION_OFFSETS[partition]);
                bus.write32(ptr + 4, FLASHROM_PARTITION_SIZES[partition]);
                LOG.info("FLASHROM_INFO: partition %d -> offset 0x%X, size 0x%X",
                        partition, FLASHROM_PARTITION_OFFSETS[partition], FLASHROM_PARTITION_SIZES[partition]);
                cpu.r[0] = 0;
                break;
            }
            case 1: { // FLASHROM_READ — r4=absolute offset, r5=dest ptr, r6=count.
                int offset = cpu.r[4];
                int count = cpu.r[6];
                if (offset < 0 || count < 0 || (long) offset + count > FLASHROM_SIZE) {
                    LOG.warn("FLASHROM_READ: out-of-range offset=0x%X count=%d - returning failure",
                            offset, count);
                    cpu.r[0] = -1;
                    break;
                }
                long destPtr = Integer.toUnsignedLong(cpu.r[5]);
                for (int i = 0; i < count; i++) {
                    bus.write8(destPtr + i, flashRom[offset + i]);
                }
                LOG.info("FLASHROM_READ: offset=0x%X count=%d", offset, count);
                cpu.r[0] = count;
                break;
            }
            case 2: { // FLASHROM_WRITE — r4=absolute offset, r5=src ptr, r6=count. Real NOR flash
                       // can only clear bits without a prior erase (confirmed against mc.pp.se and
                       // redream's own flash.c — see flashRom's own Javadoc) — modeled here with a
                       // real bitwise AND, not a plain overwrite.
                int offset = cpu.r[4];
                int count = cpu.r[6];
                if (offset < 0 || count < 0 || (long) offset + count > FLASHROM_SIZE) {
                    LOG.warn("FLASHROM_WRITE: out-of-range offset=0x%X count=%d - returning failure",
                            offset, count);
                    cpu.r[0] = -1;
                    break;
                }
                long srcPtr = Integer.toUnsignedLong(cpu.r[5]);
                for (int i = 0; i < count; i++) {
                    flashRom[offset + i] &= bus.read8(srcPtr + i);
                }
                LOG.info("FLASHROM_WRITE: offset=0x%X count=%d", offset, count);
                cpu.r[0] = count;
                break;
            }
            case 3: { // FLASHROM_DELETE — r4=the offset of a partition's own start (not a
                       // partition number). Real behavior erases that whole partition back to
                       // 0xFF (confirmed against mc.pp.se).
                int offset = cpu.r[4];
                int partition = -1;
                for (int i = 0; i < FLASHROM_PARTITION_OFFSETS.length; i++) {
                    if (FLASHROM_PARTITION_OFFSETS[i] == offset) {
                        partition = i;
                        break;
                    }
                }
                if (partition < 0) {
                    LOG.warn("FLASHROM_DELETE: 0x%X isn't the start of any known partition - "
                            + "returning failure", offset);
                    cpu.r[0] = -1;
                    break;
                }
                java.util.Arrays.fill(flashRom, offset, offset + FLASHROM_PARTITION_SIZES[partition], (byte) 0xFF);
                LOG.info("FLASHROM_DELETE: partition %d (offset 0x%X)", partition, offset);
                cpu.r[0] = 0;
                break;
            }
            default:
                LOG.warn("FLASHROM: unknown function r7=%d - returning failure", fn);
                cpu.r[0] = -1;
        }
    }

    private void handleMiscGdrom(Sh4Cpu cpu) {
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
        } else if (superFn == GDROM_SUPERFUNCTION) {
            handleGdrom(cpu, fn);
        } else {
            LOG.warn("MISC/GDROM: unknown superfunction r6=%d, function r7=%d - returning failure", superFn, fn);
            cpu.r[0] = -1;
        }
    }

    /**
     * The GDROM_* command-queue functions (see {@link #GDROM_SEND_COMMAND} etc. and this class's
     * own {@code slotStatus}/{@code slotError} Javadoc for the single-slot model used here).
     *
     * <p><b>Scope, honestly stated</b> (same discipline as the rest of this class — see the class
     * Javadoc): the queue mechanics themselves (assigning request IDs, tracking and reporting a
     * command's status/error, {@code ABORT_COMMAND}/{@code RESET}/{@code GDROM_INIT} housekeeping)
     * are implemented for real. Individual GDC_* commands are then honestly split into two
     * groups: control-only commands with no real data dependency ({@code GDC_INIT}, {@code
     * GDC_SEEK}, {@code GDC_STOP}, {@code GDC_RELEASE}, {@code GDC_REQ_MODE}, {@code
     * GDC_SET_MODE}, {@code GDC_REQ_STAT}, {@code GDC_GET_VER}) report real success, since
     * nothing about them actually requires disc data this project doesn't have. Commands that
     * would need real sector data ({@code GDC_PIOREAD}/{@code GDC_DMAREAD}), a real TOC ({@code
     * GDC_GETTOC}/{@code GDC_GETTOC2}/{@code GDC_REQ_SES}), or CDDA playback ({@code GDC_PLAY}/
     * {@code GDC_PLAY2}/{@code GDC_PAUSE}/{@code GDC_GET_SCD}) honestly report failure and log
     * why, rather than fabricating sector contents or a TOC this project doesn't have verified
     * data for. In particular, wiring {@code GDC_PIOREAD}/{@code GDC_DMAREAD} to this project's
     * already-working {@code core-gdrom} sector reading requires converting the real,
     * disc-absolute FAD (frame address) values real game code passes here into this project's
     * track-relative logical LBA (see {@code LogicalSectorReader}) — and this project doesn't yet
     * have a verified FAD base offset for the data track to do that conversion correctly. Given
     * this project has specifically been bitten before by a disc-absolute-vs-relative offset bug
     * in GD-ROM sector addressing (see this repo's top-level {@code CHANGELOG.md}'s {@code
     * core-gdrom} history), guessing at that offset here rather than confirming it first would
     * risk exactly the same class of subtle, silent bug again — left for a future pass once that
     * offset is confirmed against an authoritative source, not attempted in this one.
     */
    private void handleGdrom(Sh4Cpu cpu, int fn) {
        switch (fn) {
            case GDROM_SEND_COMMAND: {
                int commandCode = cpu.r[4];
                // r[5] (the parameter block pointer) is intentionally not read yet: every
                // command this pass actually completes is control-only and needs no parameters;
                // the one family that would need it (GDC_PIOREAD/GDC_DMAREAD's sector/count/
                // destination block) is exactly the family honestly reported as unsupported
                // below, per this method's own Javadoc.
                int requestId = nextRequestId++;
                slotRequestId = requestId;
                boolean supported = isControlOnlyGdcCommand(commandCode);
                if (supported) {
                    slotStatus = GDC_STATUS_COMPLETE;
                    slotError = GDC_ERROR_OK;
                    LOG.info("GDC command 0x%x enqueued as request %d - completed (control-only, no disc data needed)",
                            commandCode, requestId);
                } else {
                    slotStatus = GDC_STATUS_ERROR;
                    slotError = isKnownGdcCommand(commandCode) ? GDC_ERROR_SYSTEM : GDC_ERROR_INVALID_CMD;
                    LOG.warn("GDC command 0x%x enqueued as request %d - not supported yet (needs real disc "
                            + "data this project doesn't wire up here yet - see handleGdrom's Javadoc), "
                            + "reporting failure honestly rather than fabricating data", commandCode, requestId);
                }
                // SEND_COMMAND itself always "succeeds" here (the command was accepted into the
                // queue) — per mc.pp.se, "a request id (>=1) if successful" is what it returns;
                // whether the command itself later succeeds or fails is CHECK_COMMAND's job to
                // report, not this one's, matching the real async contract even though this
                // project's single slot resolves the outcome immediately.
                cpu.r[0] = requestId;
                break;
            }
            case GDROM_CHECK_COMMAND: {
                int requestId = cpu.r[4];
                int statusPtr = cpu.r[5];
                if (requestId != slotRequestId || slotRequestId == 0) {
                    LOG.warn("GDROM_CHECK_COMMAND: request id %d doesn't match the last enqueued command "
                            + "(%d) - this project's single-slot model doesn't track more than one command "
                            + "at a time - reporting GDC_STATUS_ERROR", requestId, slotRequestId);
                    cpu.r[0] = GDC_STATUS_ERROR;
                } else {
                    if (statusPtr != 0) {
                        long base = Integer.toUnsignedLong(statusPtr);
                        bus.write32(base, slotError);
                        bus.write32(base + 4, 0);
                        bus.write32(base + 8, 0);
                        bus.write32(base + 12, 0);
                    }
                    cpu.r[0] = slotStatus;
                }
                break;
            }
            case GDROM_MAINLOOP:
                // Real code calls this repeatedly to let queued commands progress. This
                // project's single slot already resolves a command's outcome synchronously at
                // SEND_COMMAND time (see this method's Javadoc), so there's genuinely nothing
                // left to progress here — a real no-op, not a stubbed-out gap.
                cpu.r[0] = 0;
                break;
            case GDROM_INIT:
                // Re-initializes the GDROM subsystem/queue — distinct from the GDC_INIT command
                // code above (that one's a queued command; this is the syscall that resets the
                // queue itself). Since this project's boot path already got this far specifically
                // because a disc image was found and opened, reporting success is accurate.
                slotStatus = GDC_STATUS_INACTIVE;
                slotError = GDC_ERROR_OK;
                slotRequestId = 0;
                LOG.info("GDROM_INIT");
                cpu.r[0] = 0;
                break;
            case GDROM_ABORT_COMMAND: {
                int requestId = cpu.r[4];
                if (requestId == slotRequestId && slotRequestId != 0) {
                    slotStatus = GDC_STATUS_INACTIVE;
                    slotError = GDC_ERROR_OK;
                    slotRequestId = 0;
                    LOG.info("GDROM_ABORT_COMMAND: aborted request %d", requestId);
                    cpu.r[0] = 0;
                } else {
                    LOG.warn("GDROM_ABORT_COMMAND: request id %d doesn't match the last enqueued command "
                            + "(%d) - nothing to abort - returning failure", requestId, slotRequestId);
                    cpu.r[0] = -1;
                }
                break;
            }
            case GDROM_RESET:
                slotStatus = GDC_STATUS_INACTIVE;
                slotError = GDC_ERROR_OK;
                slotRequestId = 0;
                nextRequestId = 1;
                LOG.info("GDROM_RESET");
                cpu.r[0] = 0;
                break;
            default:
                LOG.warn("GDROM_* (r7=%d): not implemented yet - returning failure. Real GD-ROM access "
                        + "already exists via core-gdrom's SectorSource/Iso9660FileSystem, just not "
                        + "wired into these syscalls yet.", fn);
                cpu.r[0] = -1;
        }
    }

    private static boolean isControlOnlyGdcCommand(int commandCode) {
        return commandCode == GDC_INIT || commandCode == GDC_SEEK || commandCode == GDC_STOP
                || commandCode == GDC_RELEASE || commandCode == GDC_REQ_MODE || commandCode == GDC_SET_MODE
                || commandCode == GDC_REQ_STAT || commandCode == GDC_GET_VER;
    }

    private static boolean isKnownGdcCommand(int commandCode) {
        return isControlOnlyGdcCommand(commandCode)
                || commandCode == GDC_PIOREAD || commandCode == GDC_DMAREAD
                || commandCode == GDC_GETTOC || commandCode == GDC_GETTOC2
                || commandCode == GDC_PLAY || commandCode == GDC_PLAY2 || commandCode == GDC_PAUSE
                || commandCode == GDC_READ || commandCode == GDC_GET_SCD || commandCode == GDC_REQ_SES;
    }
}
