package org.dreamjemu.cli;

import org.dreamjemu.gdrom.CdiImage;
import org.dreamjemu.gdrom.CdiTrack;
import org.dreamjemu.gdrom.CdiTrackMode;
import org.dreamjemu.gdrom.CueBinImage;
import org.dreamjemu.gdrom.CueTrack;
import org.dreamjemu.gdrom.DiscImageDetector;
import org.dreamjemu.gdrom.DiscImageFormat;
import org.dreamjemu.gdrom.GdiImage;
import org.dreamjemu.gdrom.GdiTrack;
import org.dreamjemu.gdrom.GdiTrackType;
import org.dreamjemu.gdrom.IpBinHeader;
import org.dreamjemu.gdrom.Iso9660DirectoryRecord;
import org.dreamjemu.gdrom.Iso9660FileSystem;
import org.dreamjemu.gdrom.LogicalSectorReader;
import org.dreamjemu.gdrom.SectorSource;
import org.dreamjemu.cpu.sh4.Sh4Cpu;
import org.dreamjemu.system.HleBootLoader;
import org.dreamjemu.gpu.pvr2.HollySystemRegisters;
import org.dreamjemu.gpu.pvr2.PvrRegisters;
import org.dreamjemu.system.Bus;
import org.dreamjemu.system.DreamcastAddressMap;
import org.dreamjemu.system.SystemBus;
import org.dreamjemu.common.log.LogConfig;
import org.dreamjemu.common.log.LogLevel;

import java.util.ArrayList;
import java.util.List;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A simple command-line tool for detecting, loading, and inspecting a
 * Dreamcast disc image — a lightweight, GUI-free way to sanity-check that
 * format detection, sector reading, and filesystem parsing actually work
 * against a real disc image the user has locally, while app-javafx's UI is
 * still early.
 *
 * As of this update, it goes one step further than pure inspection: after
 * locating the boot file, it reads its bytes, loads them into a real
 * {@link SystemBus} at the documented Dreamcast boot address, and points a
 * fresh {@link Sh4Cpu} at that address — the project's first minimally real,
 * BIOS-free boot attempt (see {@link org.dreamjemu.system.HleBootLoader}).
 * <b>This still does not run a real game to completion</b> — the interpreter
 * only implements a subset of the SH-4 ISA so far (no {@code TRAPA}, no MMU,
 * no real hardware register behavior), so execution is expected to stop as
 * soon as real boot code needs any of that; this tool reports exactly where
 * and why it stopped rather than hiding the gap. See /docs/STATUS.md and
 * /docs/ROADMAP.md for what's implemented.
 *
 * Usage: point it at a disc image file the user already legally owns — this
 * project does not provide, link to, or facilitate finding any disc images
 * itself; see README.md's stance on piracy.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        List<String> positional = new ArrayList<>();
        String logLevelArg = null;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--log-level=")) {
                logLevelArg = arg.substring("--log-level=".length());
            } else if (arg.equals("--log-level")) {
                if (i + 1 >= args.length) {
                    System.err.println("--log-level requires a value (TRACE, DEBUG, INFO, WARN, ERROR, or OFF)");
                    System.exit(1);
                    return;
                }
                logLevelArg = args[++i];
            } else {
                positional.add(arg);
            }
        }

        if (logLevelArg != null) {
            try {
                LogConfig.setGlobalLevel(LogLevel.valueOf(logLevelArg.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                System.err.println("Unrecognized --log-level value: \"" + logLevelArg
                        + "\" (expected TRACE, DEBUG, INFO, WARN, ERROR, or OFF)");
                System.exit(1);
                return;
            }
        }

        if (positional.isEmpty()) {
            printUsage();
            System.exit(1);
            return;
        }

        Path imagePath = Path.of(positional.get(0));
        if (!Files.isRegularFile(imagePath)) {
            System.err.println("File not found: " + imagePath);
            System.exit(1);
        }

        DiscImageFormat format = DiscImageDetector.detect(imagePath);
        System.out.println("File            : " + imagePath.toAbsolutePath());
        System.out.println("Detected format : " + format);

        try {
            OpenDataTrack dataTrack = switch (format) {
                case GDI -> openGdiDataTrack(imagePath);
                case CUE_BIN -> openCueBinDataTrack(imagePath);
                case CDI -> openCdiDataTrack(imagePath);
                case CHD, UNKNOWN -> null;
            };

            if (dataTrack == null) {
                System.out.println();
                System.out.println("Sector reading is not implemented yet for format: " + format);
                System.out.println("GDI, CUE/BIN, and CDI reading exist so far — see docs/STATUS.md.");
                System.exit(2);
                return;
            }

            try (dataTrack) {
                byte[] headerSector = new byte[LogicalSectorReader.LOGICAL_SECTOR_SIZE];
                dataTrack.reader().readSector(0, headerSector);
                byte[] header = new byte[IpBinHeader.HEADER_SIZE];
                System.arraycopy(headerSector, 0, header, 0, IpBinHeader.HEADER_SIZE);

                IpBinHeader ipBin = IpBinHeader.parse(header);
                printIpBinHeader(ipBin);
                printBootFileLocation(dataTrack.reader(), ipBin);
            }
        } catch (Exception e) {
            System.err.println("Failed to read disc image: " + e.getMessage());
            System.exit(3);
        }
    }

    /** Pairs a normalized sector reader with the underlying disc image resource it depends on, so both can be closed together. */
    private record OpenDataTrack(LogicalSectorReader reader, AutoCloseable image) implements AutoCloseable {
        @Override
        public void close() throws Exception {
            image.close();
        }
    }

    private static void printUsage() {
        System.out.println("DreamJEmu disc image inspector (CLI)");
        System.out.println();
        System.out.println("Usage: dreamjemu-cli [--log-level LEVEL] <path-to-disc-image.gdi|.cue|.cdi>");
        System.out.println();
        System.out.println("Detects the disc image format, loads it, locates the data track, prints");
        System.out.println("the parsed IP.BIN boot header, locates the boot file it names within the");
        System.out.println("disc's ISO9660 root directory, loads its bytes into a real system bus at");
        System.out.println("the documented Dreamcast boot address, and steps the SH-4 from there until");
        System.out.println("it hits something not implemented yet, or a stable repeating loop is detected");
        System.out.println("(see LoopDetector — stops early with a summary rather than exhausting the step");
        System.out.println("budget on what's often just a slow, legitimate memset/.bss-clear loop). Point");
        System.out.println("it at a disc image you already legally own - this project does not provide or");
        System.out.println("link to any.");
        System.out.println();
        System.out.println("This does not run a real game to completion yet; see docs/STATUS.md.");
        System.out.println();
        System.out.println("--log-level LEVEL   TRACE, DEBUG, INFO (default), WARN, ERROR, or OFF.");
        System.out.println("                     TRACE logs every SH-4 instruction executed (PC + opcode) -");
        System.out.println("                     very verbose against real disc images (millions of lines).");
        System.out.println("                     Can also be set via -Ddreamjemu.log.level=LEVEL or the");
        System.out.println("                     DREAMJEMU_LOG_LEVEL environment variable; this flag wins");
        System.out.println("                     if more than one is set. See common's LogConfig.");
    }

    /**
     * Dreamcast GD-ROM images (in every format this project reads) commonly
     * contain more than one non-audio track: a small "single-density" area
     * first (present only so ordinary CD-ROM drives see a valid-looking
     * disc — it is NOT the game), then an audio track, then the real
     * "high-density" game data as the LAST track on the disc. Picking the
     * first data track (an earlier, naive version of this code did exactly
     * that) silently opens the wrong, mostly-empty filesystem instead of the
     * one containing the boot file — see docs/STATUS.md's "Known
     * limitations fixed" note for how this was diagnosed against a real
     * Sonic Adventure dump (its single-density track only contains the
     * standard ISO9660 ABSTRACT.TXT/BIBLIOGR.TXT/COPYRIGH.TXT placeholder
     * files and an EXTRA directory — no boot file at all). Picking the LAST
     * data track instead is the same convention real Dreamcast tools and
     * emulators (redream, Flycast, etc.) already rely on.
     */
    private static OpenDataTrack openGdiDataTrack(Path path) throws IOException {
        GdiImage image = GdiImage.load(path);
        GdiTrack lastDataTrack = null;
        for (GdiTrack track : image.tracks()) {
            if (track.type() == GdiTrackType.DATA) {
                lastDataTrack = track;
            }
        }
        if (lastDataTrack == null) {
            image.close();
            throw new IOException("No data track found in " + path);
        }
        return new OpenDataTrack(new LogicalSectorReader(image::readSector, lastDataTrack.startLba(), lastDataTrack.sectorSize()), image);
    }

    /** See {@link #openGdiDataTrack} — same "pick the last data track" reasoning applies here. */
    private static OpenDataTrack openCueBinDataTrack(Path path) throws IOException {
        CueBinImage image = CueBinImage.load(path);
        CueTrack lastDataTrack = null;
        for (CueTrack track : image.tracks()) {
            if (!track.mode().isAudio()) {
                lastDataTrack = track;
            }
        }
        if (lastDataTrack == null) {
            image.close();
            throw new IOException("No data track found in " + path);
        }
        return new OpenDataTrack(new LogicalSectorReader(image::readSector, lastDataTrack.startLba(), lastDataTrack.sectorSize()), image);
    }

    /** See {@link #openGdiDataTrack} — same "pick the last data track" reasoning applies here. */
    private static OpenDataTrack openCdiDataTrack(Path path) throws IOException {
        CdiImage image = CdiImage.load(path);
        CdiTrack lastDataTrack = null;
        for (CdiTrack track : image.tracks()) {
            if (track.mode() != CdiTrackMode.CDDA) {
                lastDataTrack = track;
            }
        }
        if (lastDataTrack == null) {
            image.close();
            throw new IOException("No data track found in " + path);
        }
        return new OpenDataTrack(new LogicalSectorReader(image::readSector, lastDataTrack.startLba(), lastDataTrack.sectorSize()), image);
    }

    private static void printIpBinHeader(IpBinHeader ipBin) {
        System.out.println();
        System.out.println("=== IP.BIN boot header ===");
        System.out.println("Hardware ID      : " + ipBin.hardwareId()
                + (ipBin.isValidDreamcastHeader() ? "  [OK]" : "  [UNEXPECTED - is this really a Dreamcast disc image?]"));
        System.out.println("Maker ID         : " + ipBin.makerId());
        System.out.println("Device info      : " + ipBin.deviceInfo());
        System.out.println("Area symbols     : " + ipBin.areaSymbols());
        System.out.println("Peripherals      : " + ipBin.peripherals());
        System.out.println("Product number   : " + ipBin.productNumber());
        System.out.println("Product version  : " + ipBin.productVersion());
        System.out.println("Release date     : " + ipBin.releaseDate());
        System.out.println("Boot filename    : " + ipBin.bootFilename());
        System.out.println("Software company : " + ipBin.softwareCompany());
        System.out.println("Software name    : " + ipBin.softwareName());
    }

    private static void printBootFileLocation(SectorSource dataTrack, IpBinHeader ipBin) {
        System.out.println();
        System.out.println("=== ISO9660 boot file lookup ===");
        try {
            Iso9660FileSystem fs = Iso9660FileSystem.open(dataTrack);
            Iso9660DirectoryRecord bootFile = fs.findInRootDirectory(ipBin.bootFilename());
            System.out.println("Found \"" + ipBin.bootFilename() + "\" at LBA " + bootFile.extentLba()
                    + ", " + bootFile.dataLength() + " bytes.");
            attemptMinimalBoot(fs, bootFile, ipBin);
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            System.out.println("Could not locate the boot file: " + message);
            printRootDirectoryDiagnostics(dataTrack);
        }
    }

    /**
     * Diagnostic aid for when {@link Iso9660FileSystem#findInRootDirectory}
     * fails against a real disc image: prints the root directory's own
     * extent and every entry actually found in it, so a genuine mismatch
     * (wrong name, empty/garbled listing, wrong extent) can be told apart
     * from a real parsing bug without guessing. Deliberately swallows its
     * own failures — this only runs after something already went wrong, and
     * shouldn't itself crash the tool or hide the original error above.
     */
    private static void printRootDirectoryDiagnostics(SectorSource dataTrack) {
        System.out.println();
        System.out.println("--- diagnostic: root directory contents ---");
        try {
            Iso9660FileSystem fs = Iso9660FileSystem.open(dataTrack);
            Iso9660DirectoryRecord root = fs.rootDirectory();
            System.out.println("Root directory extent: LBA " + root.extentLba() + ", " + root.dataLength() + " bytes");
            var entries = fs.listRootDirectory();
            System.out.println("Entries found: " + entries.size());
            for (Iso9660DirectoryRecord entry : entries) {
                System.out.println("  " + (entry.isDirectory() ? "[DIR] " : "       ") + entry.identifier()
                        + "  (LBA " + entry.extentLba() + ", " + entry.dataLength() + " bytes)");
            }
        } catch (Exception e) {
            System.out.println("(diagnostic listing also failed: "
                    + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()) + ")");
        }
    }

    /**
     * The project's first minimally real, BIOS-free boot attempt: reads the
     * located boot file's bytes, loads them into a real {@link SystemBus} at
     * the documented Dreamcast boot address (see {@link HleBootLoader}), and
     * steps a fresh {@link Sh4Cpu} from there until it hits something this
     * interpreter doesn't implement yet (expected — see /docs/STATUS.md for
     * exactly how much of the SH-4 ISA exists so far) or a step budget is
     * reached. Reports exactly where and why execution stopped rather than
     * claiming success it can't back up.
     */
    private static void attemptMinimalBoot(Iso9660FileSystem fs, Iso9660DirectoryRecord bootFile, IpBinHeader ipBin) {
        System.out.println();
        System.out.println("=== Loading boot file and starting the SH-4 ===");

        byte[] bootFileBytes;
        try {
            bootFileBytes = fs.readFile(bootFile);
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            System.out.println("Could not read the boot file's bytes: " + message);
            return;
        }

        SystemBus bus = new SystemBus();
        int entryPc = HleBootLoader.loadBootFile(bus, bootFileBytes);
        System.out.println("Loaded " + bootFileBytes.length + " bytes to 0x" + Integer.toHexString(entryPc)
                + " (real hardware's documented \"" + ipBin.bootFilename() + "\" load address).");

        // Added 2026-08-10 after a real Sonic Adventure run reached a stable, budget-exhausting
        // spin-wait on PVR2's SPG_STATUS register (see PvrRegisters' own Javadoc for the full
        // real-world context and why only SPG_STATUS is modeled, and only approximately).
        // Without this, that address falls into the generic UnmappedRegion catch-all below,
        // which always reads 0 — exactly the loop's failure mode. Kept as a named variable (not
        // just handed to mapRegion anonymously) so the step loop below can call tick() on it —
        // see PvrRegisters.tick()'s own Javadoc for why that has to be driven by real elapsed
        // steps, not by SPG_STATUS being read.
        PvrRegisters pvrRegisters = new PvrRegisters();
        bus.mapRegion(DreamcastAddressMap.PVR2_REGISTERS_BASE, DreamcastAddressMap.PVR2_REGISTERS_SIZE,
                pvrRegisters);

        // Added 2026-08-11 after a real Sonic Adventure run reached a second, immediately
        // following spin-wait, this time on Holly's SB_ISTNRM (VBLANK_BEGIN) — see
        // HollySystemRegisters' own Javadoc for the full real-world context. Kept as a named
        // variable for the same reason pvrRegisters is: the step loop below calls
        // setVblankBeginPending() on it exactly when pvrRegisters.tick() reports a real VBlank.
        HollySystemRegisters hollySystemRegisters = new HollySystemRegisters();
        bus.mapRegion(DreamcastAddressMap.HOLLY_SYSTEM_REGISTERS_BASE,
                DreamcastAddressMap.HOLLY_SYSTEM_REGISTERS_SIZE, hollySystemRegisters);

        // Added 2026-08-04: a real BIOS installs this vector table before jumping to a game;
        // this BIOS-free HLE boot never did, so any code trying to use a BIOS syscall would read
        // zero from an uninitialized vector. See BiosSyscallHandler's Javadoc for what's actually
        // implemented behind each vector (the state-free ones only — flashrom/font-ROM/real
        // GD-ROM command processing are honestly reported as unsupported, not faked).
        BiosSyscallHandler.installVectorTable(bus);
        BiosSyscallHandler biosSyscalls = new BiosSyscallHandler(bus);

        Sh4Cpu cpu = new Sh4Cpu(bus, entryPc);

        // Added after a real Sonic Adventure run stopped at PC=0x00000000 (2026-08-04): PR
        // defaults to 0 (Sh4Cpu's own Java int default), and this BIOS-free HLE boot never sets
        // it, unlike real hardware's BIOS. See HleBootLoader.BOOT_RETURN_SENTINEL's own Javadoc
        // for the full reasoning — in short, this lets the eventual RTS that unwinds out of the
        // boot entry's outermost call frame land somewhere this loop can recognize on purpose
        // (below) and report as a clean unwind, instead of stopping on an unrecognized PC=0.
        cpu.pr = HleBootLoader.BOOT_RETURN_SENTINEL;

        // Added 2026-08-09 after the BOOT_RETURN_SENTINEL diagnostic above (see its own comment
        // just below, and HleBootLoader.INITIAL_STACK_POINTER's Javadoc) revealed the actual root
        // cause of the real Sonic Adventure run's PC=0 stop: R15 (the stack pointer), like PR,
        // also defaults to 0 and was never set -- every push from R15=0 wraps around to a huge
        // address near 0xFFFFFFFF instead of a real RAM address, corrupting whatever real data
        // (including saved copies of PR) a real stack would have held there.
        cpu.r[15] = HleBootLoader.INITIAL_STACK_POINTER;

        // Added 2026-08-15 after a real Sonic Adventure run confirmed (via a dedicated LDC Rn,SR
        // logging diagnostic — see that opcode's own comment) that real game code never lowers
        // SR.IMASK itself across the entire observed boot sequence, the same "assumes the real
        // BIOS already handled this" pattern as PR/r[15] above — see
        // HleBootLoader.INITIAL_SR's own Javadoc for the full real-execution evidence this rests
        // on. Without this, SR stays at Sh4Cpu's own real-hardware reset value (IMASK=1111,
        // fully masked), and no real interrupt (including the VBlank one PvrRegisters.tick()/
        // HollySystemRegisters below try to deliver every step) can ever actually reach the CPU.
        cpu.setSr(HleBootLoader.INITIAL_SR);
        // Step budget history against the real Sonic Adventure dump, all on 2026-07-31:
        //   100,000    -> reached with no unimplemented instruction hit at all.
        //   5,000,000  -> also reached, twice more (once per LoopDetector refinement below).
        //   LoopDetector ultimately resolved the ambiguity this kept causing: it's genuinely
        //   two sequential, legitimate crt0-style fill loops, not a spin-wait. The interpreter
        //   got stuck reporting the SAME loop (byte-at-a-time, ~1 byte/5 instructions) for
        //   nearly the entire 5,000,000 budget, clearing on the order of ~1MB — plausible for a
        //   real .bss/buffer region, just slow. 100,000,000 gives real room for that to finish.
        final int maxSteps = 100_000_000;

        // Added 2026-07-31: a --log-level TRACE run against the real dump revealed the FIRST of
        // these fill loops (a tight, stable 5-instruction, byte-at-a-time loop). Detecting that
        // early (instead of silently burning the whole budget, or a TRACE log, on identical
        // repeated output) is what LoopDetector is for — see its Javadoc for the algorithm.
        //
        // Refined twice more the same day, per the project owner, each time based on what an
        // actual real-dump run showed:
        //   1. "Report once, then stop checking and keep stepping" — a second real run found a
        //      SECOND, earlier, faster (4-byte-at-a-time) fill loop, confirming there can be more
        //      than one.
        //   2. "Keep detection continuously active, only re-report a genuinely different loop" —
        //      that run then reached the WHOLE step budget again, and with detection switched off
        //      after the first report, there was no way to tell whether execution was still stuck
        //      in the very same loop the whole time, had moved on to a different one, or had
        //      reached genuinely new code that just happened to run long. A further real run with
        //      THIS refinement finally gave an unambiguous answer: exactly 2 distinct loops seen,
        //      stuck in the second (the byte-at-a-time one) for the rest of the budget — which is
        //      exactly what justified raising maxSteps again, above, instead of investigating
        //      further: we now know this is real, finite work, not a hardware-modeling gap.
        LoopDetector loopDetector = new LoopDetector(1000, 1_000_000);
        final int ringCapacity = 4096; // generous upper bound on a loop body we can still fully display
        int[] pcRing = new int[ringCapacity];
        int[] opcodeRing = new int[ringCapacity];
        int ringPushes = 0;
        int lastReportedLoopIdentity = 0;
        boolean haveLastReportedLoop = false;
        int lastReportedAtStep = -1;
        int distinctLoopsReported = 0;

        int steps = 0;
        try {
            for (; steps < maxSteps; steps++) {
                if (cpu.pc == HleBootLoader.BOOT_RETURN_SENTINEL) {
                    // See HleBootLoader.BOOT_RETURN_SENTINEL's Javadoc: this is a clean unwind
                    // out of the boot entry's outermost call frame, not a crash or a missing
                    // instruction — recognized on purpose, not stumbled into like the PC=0 case
                    // this replaces (see the PC==0 branch in the catch block below, kept as a
                    // fallback in case PR ever legitimately ends up 0 some other way).
                    System.out.println();
                    System.out.println("Execution returned cleanly to the boot-entry sentinel after " + steps
                            + " step(s) -");
                    System.out.println("this is the boot entry's outermost RTS unwinding out, matching real hardware's");
                    System.out.println("own call/return convention, not a crash or a missing instruction. See");
                    System.out.println("HleBootLoader.BOOT_RETURN_SENTINEL's Javadoc and docs/STATUS.md.");
                    printRecentHistory(pcRing, opcodeRing, ringPushes, ringCapacity, 40);
                    return;
                }
                if (BiosSyscallHandler.isSyscallTrap(cpu.pc)) {
                    // No real opcode exists at a trap address (it's not real BIOS code — see
                    // BiosSyscallHandler's Javadoc) — record it in the history ring buffer as a
                    // recognizable sentinel, handle it natively, and skip straight to the next
                    // iteration rather than letting the normal fetch/decode path see opcode 0x0000
                    // there and fail exactly the way the PC=0 boundary did.
                    pcRing[ringPushes % ringCapacity] = cpu.pc;
                    opcodeRing[ringPushes % ringCapacity] = 0x0000;
                    ringPushes++;
                    biosSyscalls.handle(cpu);
                    continue;
                }

                int pcBeforeStep = cpu.pc;
                int opcodeBeforeStep = bus.read16(Integer.toUnsignedLong(pcBeforeStep)) & 0xFFFF;
                pcRing[ringPushes % ringCapacity] = pcBeforeStep;
                opcodeRing[ringPushes % ringCapacity] = opcodeBeforeStep;
                ringPushes++;

                if (loopDetector.observe(pcBeforeStep, steps)) {
                    int identity = identifyLoop(pcRing, ringPushes, ringCapacity, loopDetector.period());
                    if (!haveLastReportedLoop || identity != lastReportedLoopIdentity) {
                        if (haveLastReportedLoop) {
                            System.out.println("Left the previously reported loop after " + (steps - lastReportedAtStep)
                                    + " steps — this is a DIFFERENT loop:");
                        }
                        printLoopDetected(loopDetector, pcRing, opcodeRing, ringPushes, ringCapacity, steps, cpu,
                                hollySystemRegisters, bus);
                        lastReportedLoopIdentity = identity;
                        distinctLoopsReported++;
                    }
                    haveLastReportedLoop = true;
                    lastReportedAtStep = steps;
                    loopDetector.reset(); // keep checking for whatever comes next, same loop or not
                }
                cpu.step();

                // Added 2026-08-12 after a real Sonic Adventure run reached a self-referential
                // BRA (an infinite idle spin) right after code that set up interrupt-related
                // hardware state — genuine "wait for a real interrupt" code, which no amount of
                // faking register *read* values (what this project's earlier PvrRegisters/
                // HollySystemRegisters versions did) can ever satisfy. This ticks real,
                // autonomous video timing once per step (see PvrRegisters.tick()'s Javadoc for
                // why it must be driven this way, not by SPG_STATUS being read), and on a real
                // VBlank, sets the real interrupt-status bit — exactly what real Holly hardware
                // does, whether or not this specific idle loop is even the code running at the
                // time.
                if (pvrRegisters.tick()) {
                    hollySystemRegisters.setVblankBeginPending();
                }
                // Real hardware keeps a pending Holly interrupt line continuously asserted until
                // the CPU actually accepts it (which might not be the same step it became
                // pending — e.g. if interrupts are still masked at that moment, and only
                // unmasked later) — so this tries every step, not just the one tick() fired on,
                // exactly matching that real "keeps asking until accepted" behavior rather than
                // only offering the interrupt once and giving up.
                //
                // Added 2026-08-16, refining the fix HleBootLoader.INITIAL_SR made: a real run
                // confirmed the interrupt genuinely fires now (PC correctly jumped to
                // vbr + 0x600), but VBR was still 0 (Sh4Cpu's own real hardware reset value —
                // "starts at 0, same as real hardware out of reset", see vbr's own Javadoc) at
                // that point, since real game code hadn't executed its own LDC Rn,VBR yet to
                // install its own vector table — so the CPU jumped into genuinely unmapped
                // memory (opcode 0x0000, an immediate crash) rather than a real handler. Real
                // hardware wouldn't hit this: by the time interrupts are truly unmasked, a valid
                // vector table (the BIOS's own, at minimum) is already in place — this project's
                // HLE boot has no equivalent BIOS vector table to install, so the safe, still
                // real-hardware-grounded equivalent is: don't try delivering an interrupt until
                // real code has actually pointed VBR somewhere itself (a non-zero VBR is real
                // code's own signal that it's ready to receive one — LDC Rn,VBR is the only way
                // VBR ever changes, so this can't fire prematurely before that).
                if (cpu.vbr != 0 && hollySystemRegisters.hasPendingNormalInterrupt()) {
                    cpu.tryDeliverInterrupt(HollySystemRegisters.NORMAL_INTERRUPT_PRIORITY_LEVEL,
                            HollySystemRegisters.NORMAL_INTERRUPT_INTEVT);
                }
            }

            System.out.println("Executed " + steps + " steps without hitting an unimplemented instruction.");
            if (haveLastReportedLoop) {
                System.out.println("(Reached the step budget " + (steps - lastReportedAtStep)
                        + " steps after the loop last reported above — that was still the SAME loop the whole");
                System.out.println(" time (" + distinctLoopsReported + " distinct loop(s) seen in total this run);");
                System.out.println(" execution never left it within the budget.)");
            } else {
                System.out.println("(Reached the step budget rather than a real stopping condition - this");
                System.out.println(" interpreter found no stable repeating loop either; genuinely new code");
                System.out.println(" the whole way, or a loop longer than LoopDetector's repeat threshold.)");
            }
        } catch (UnsupportedOperationException | IllegalStateException e) {
            System.out.println("Stopped after " + steps + " step(s) at PC=0x" + Integer.toHexString(cpu.pc) + ":");
            System.out.println("  " + e.getMessage());
            if (haveLastReportedLoop) {
                System.out.println("(This happened " + (steps - lastReportedAtStep) + " steps after the loop");
                System.out.println(" last reported above (" + distinctLoopsReported + " distinct loop(s) seen in");
                System.out.println(" total) — execution DID eventually leave it and reach new code before hitting this.)");
            }
            if (cpu.pc == 0) {
                // Kept as a fallback: since HleBootLoader.BOOT_RETURN_SENTINEL was added (see
                // above), a clean unwind out of the boot entry's outermost call frame should be
                // caught by that check instead, before ever reaching here. Landing on PC=0
                // now would mean PR held 0 for some OTHER reason (e.g. a real bug elsewhere
                // clobbering PR, or code that reads an uninitialized/zeroed stack slot as a
                // return address directly, bypassing the sentinel this project set at boot) —
                // still worth flagging explicitly rather than folding into the generic message
                // below, but no longer the expected, understood case it was before the sentinel.
                //
                // Found via a real Sonic Adventure run: the last instruction before this was
                // RTS, returning to a PR value of exactly 0 — Sh4Cpu.pr's Java default, since
                // nothing ever explicitly set it before the sentinel fix above. On real
                // hardware, the BIOS sets PR to a valid "return to supervisor code" address
                // before jumping to a game's entry point. 0x00000000 is never a legitimate
                // fetch address otherwise (RAM starts at 0x0C000000; the boot file loads to
                // 0x8C010000+), so landing here specifically remains a strong, if not certain, signal.
                System.out.println();
                System.out.println("PC=0 specifically: on real hardware this address is never legitimately");
                System.out.println("fetched from. Now that HleBootLoader.BOOT_RETURN_SENTINEL is set on PR before");
                System.out.println("execution starts, a clean unwind out of the boot entry should be caught above");
                System.out.println("instead of reaching here — if you're seeing this, something else zeroed PR.");
                System.out.println("See docs/STATUS.md.");
            } else {
                System.out.println("This is expected at this stage: real boot code needs SH-4 instructions,");
                System.out.println("hardware registers, or an exception/interrupt mechanism (TRAPA, MMU) this");
                System.out.println("interpreter doesn't implement yet. See docs/STATUS.md \"Not started yet\".");
            }
            printRecentHistory(pcRing, opcodeRing, ringPushes, ringCapacity, 40);
        }
    }

    /**
     * Dumps the last {@code count} (PC, opcode) pairs executed before a stop, oldest first —
     * added specifically because a real run stopped at {@code PC=0x00000000} (opcode
     * {@code 0x0000}, itself not a valid instruction — almost certainly execution having jumped
     * through an uninitialized function pointer or vector-table entry a real BIOS would have set
     * up), which is far more useful to diagnose with the handful of instructions leading up to it
     * than with just the single failing address. Reuses the same ring buffer {@link LoopDetector}
     * already keeps for loop-body display, so this costs nothing extra to maintain.
     */
    private static void printRecentHistory(int[] pcRing, int[] opcodeRing, int ringPushes, int ringCapacity, int count) {
        int available = Math.min(ringPushes, Math.min(count, ringCapacity));
        System.out.println();
        System.out.println("--- last " + available + " instruction(s) executed before this ---");
        for (int i = available - 1; i >= 0; i--) {
            int index = ((ringPushes - 1 - i) % ringCapacity + ringCapacity) % ringCapacity;
            System.out.println(String.format("    PC=0x%08X opcode=0x%04X", pcRing[index], opcodeRing[index]));
        }
    }

    /**
     * Reports a {@link LoopDetector}-confirmed stable cycle: the loop's period and how many
     * consecutive repeats confirmed it, plus (PC, opcode) for every instruction in one full
     * period, reconstructed from the ring buffer {@code attemptMinimalBoot} kept alongside
     * stepping — {@link LoopDetector} itself tracks only PC-to-step-index mappings, not the
     * PC sequence in order, so it can't reconstruct the loop body on its own.
     *
     * <p>Also dumps every general-purpose register's value at the moment of detection. Added
     * specifically because a real Sonic Adventure run reached a stable 3-instruction loop
     * ({@code MOV.L @R4,R3} / {@code TST R5,R3} / {@code BT -4}) whose shape — read a
     * memory-mapped value, test a bit, branch back while it's clear — is the classic
     * "poll a hardware status register until a bit sets" pattern (VBlank, DMA completion, a
     * timer), rather than legitimate bounded work (a {@code memset}/{@code .bss}-clear loop):
     * those terminate because a *counter register* changes every iteration; this class of loop
     * only terminates when *external, unmodeled hardware* changes a bit this interpreter has no
     * way to ever set. Distinguishing the two from the loop body's opcodes alone is a guess; the
     * one piece of information that actually answers it is what address is being polled — which
     * requires seeing the actual register values, not just which registers are named. Kept as a
     * general diagnostic (not special-cased to this one loop's exact opcodes) since any future
     * loop this shape could be the same category of gap.
     */
    private static void printLoopDetected(LoopDetector loopDetector, int[] pcRing, int[] opcodeRing,
                                           int ringPushes, int ringCapacity, int steps, Sh4Cpu cpu,
                                           HollySystemRegisters hollySystemRegisters, Bus bus) {
        long period = loopDetector.period();
        System.out.println("Detected a stable repeating loop after " + steps + " total steps:");
        System.out.println("  Period: " + period + " instruction(s), confirmed over "
                + loopDetector.consecutiveRepeats() + " consecutive repeats.");

        long available = Math.min(ringPushes, ringCapacity);
        if (period <= 0 || period > available) {
            System.out.println("  (Loop body too long to display fully — period is " + period
                    + " instructions, only the last " + available + " are kept.)");
        } else {
            System.out.println("  Loop body:");
            for (long i = period - 1; i >= 0; i--) {
                int index = (int) (((long) ringPushes - 1 - i) % ringCapacity);
                if (index < 0) {
                    index += ringCapacity;
                }
                int loopPc = pcRing[index];
                int loopOpcode = opcodeRing[index];
                System.out.println(String.format("    PC=0x%08X opcode=0x%04X", loopPc, loopOpcode));
                // Added 2026-08-14: delayed branches (BRA/BSR/JSR/JMP/RTS/RTE/BT.S/BF.S) execute
                // a delay-slot instruction at PC+2 BEFORE the jump — but Main only pushes one
                // ring-buffer entry per Sh4Cpu.step() call, so that delay-slot instruction was
                // otherwise invisible in this trace (a real BRA -2 idle loop still executes
                // whatever is in its delay slot every single iteration; only the outer branch
                // itself is unconditional). A plain, side-effect-free extra fetch (mirroring
                // Sh4Cpu.fetch()'s own bus.read16 exactly) makes that visible.
                if (isDelayedBranchOpcode(loopOpcode)) {
                    int slotOpcode = bus.read16(Integer.toUnsignedLong(loopPc + 2)) & 0xFFFF;
                    System.out.println(String.format("      (delay slot) PC=0x%08X opcode=0x%04X",
                            loopPc + 2, slotOpcode));
                }
            }
        }

        System.out.println("  Register snapshot at detection (for identifying a hardware-register-poll");
        System.out.println("  address, if this turns out to be a spin-wait rather than bounded work):");
        for (int i = 0; i < 16; i += 4) {
            System.out.println(String.format("    R%-2d=0x%08X  R%-2d=0x%08X  R%-2d=0x%08X  R%-2d=0x%08X",
                    i, cpu.r[i], i + 1, cpu.r[i + 1], i + 2, cpu.r[i + 2], i + 3, cpu.r[i + 3]));
        }

        // Added 2026-08-13 after real interrupt delivery (Sh4Cpu.tryDeliverInterrupt) still
        // didn't unblock the BRA -2 idle loop found in the previous session — this is the one
        // piece of information that actually answers why: whether the CPU currently has
        // interrupts masked (BL/IMASK), and whether a real Holly interrupt is even pending right
        // now (hasPendingNormalInterrupt()), from this project's actual PvrRegisters.tick()-driven
        // timing. Distinguishing "interrupts are (correctly) still masked at this point" from
        // "an interrupt is pending and unmasked but delivery still isn't happening" (a genuine
        // bug) needs this, not just the general-purpose registers above.
        System.out.println(String.format(
                "  Interrupt state: SR.T=%s SR.BL=%s SR.IMASK=%d  Holly normal-interrupt pending=%s",
                cpu.tFlag(), cpu.blFlag(), cpu.imaskLevel(), hollySystemRegisters.hasPendingNormalInterrupt()));

        System.out.println("This may be legitimate but slow work (e.g. a byte-at-a-time memset/.bss-clear");
        System.out.println("loop — see docs/STATUS.md) or a genuine spin-wait on hardware state this");
        System.out.println("interpreter doesn't model yet (VBlank, DMA, timers — none exist so far).");
        System.out.println("Continuing to step past this point — will only report again if a genuinely");
        System.out.println("different loop is found, or speak up if something unimplemented is hit.");
    }

    /**
     * Identifies a just-confirmed loop by the lowest PC in its body — real, compiled loop bodies
     * essentially never coincidentally share their lowest instruction address with a genuinely
     * different loop, so this is a cheap, good-enough signature for telling "still the same loop"
     * apart from "moved on to a different one" across repeated {@link LoopDetector} detections
     * (see {@code attemptMinimalBoot}'s comment on why detection is kept continuously active
     * rather than switched off after the first report). Falls back to just the most recent PC if
     * the loop body is too long to fully reconstruct from the ring buffer — an imperfect but safe
     * degradation (worst case, two large loops get treated as different when they're not).
     */
    /**
     * Mirrors {@code Sh4Cpu.isBranchOpcode} exactly (that one's private) — see this file's
     * {@code printLoopDetected} for why this diagnostic needs to know which opcodes have a
     * delay slot worth also fetching and displaying.
     */
    private static boolean isDelayedBranchOpcode(int opcode) {
        return (opcode & 0xF000) == 0xA000   // BRA
                || (opcode & 0xF000) == 0xB000  // BSR
                || (opcode & 0xF0FF) == 0x400B  // JSR
                || (opcode & 0xF0FF) == 0x402B  // JMP
                || opcode == 0x000B             // RTS
                || opcode == 0x002B             // RTE
                || (opcode & 0xFF00) == 0x8D00  // BT/S
                || (opcode & 0xFF00) == 0x8F00; // BF/S
    }

    private static int identifyLoop(int[] pcRing, int ringPushes, int ringCapacity, long period) {
        long available = Math.min(ringPushes, ringCapacity);
        if (period <= 0 || period > available) {
            return pcRing[(ringPushes - 1) % ringCapacity];
        }
        int lowestPc = Integer.MAX_VALUE;
        for (long i = 0; i < period; i++) {
            int index = (int) (((long) ringPushes - 1 - i) % ringCapacity);
            if (index < 0) {
                index += ringCapacity;
            }
            lowestPc = Math.min(lowestPc, pcRing[index]);
        }
        return lowestPc;
    }
}
