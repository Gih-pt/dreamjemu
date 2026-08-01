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

        Sh4Cpu cpu = new Sh4Cpu(bus, entryPc);
        // Raised from 100,000 on 2026-07-31: a real Sonic Adventure dump reached that
        // original budget with no unimplemented instruction hit at all — genuinely
        // encouraging, but it means 100,000 wasn't a real ceiling, just an arbitrary one.
        final int maxSteps = 5_000_000;

        // Added 2026-07-31: that same real dump then ran the ENTIRE 5,000,000-step budget
        // without stopping — a --log-level TRACE run revealed why: a tight, stable 5-instruction
        // loop (almost certainly a crt0-style byte-at-a-time .bss-clearing loop — legitimate,
        // finite work, just far too slow to finish within any reasonable step budget). Detecting
        // that early (instead of silently burning the whole budget, or a TRACE log, on identical
        // repeated output) is what LoopDetector is for — see its Javadoc for the algorithm.
        LoopDetector loopDetector = new LoopDetector(1000, 1_000_000);
        final int ringCapacity = 4096; // generous upper bound on a loop body we can still fully display
        int[] pcRing = new int[ringCapacity];
        int[] opcodeRing = new int[ringCapacity];
        int ringPushes = 0;

        int steps = 0;
        boolean loopDetected = false;
        try {
            for (; steps < maxSteps; steps++) {
                int pcBeforeStep = cpu.pc;
                int opcodeBeforeStep = bus.read16(Integer.toUnsignedLong(pcBeforeStep)) & 0xFFFF;
                pcRing[ringPushes % ringCapacity] = pcBeforeStep;
                opcodeRing[ringPushes % ringCapacity] = opcodeBeforeStep;
                ringPushes++;

                if (loopDetector.observe(pcBeforeStep, steps)) {
                    loopDetected = true;
                    break;
                }
                cpu.step();
            }

            if (loopDetected) {
                printLoopDetected(loopDetector, pcRing, opcodeRing, ringPushes, ringCapacity, steps);
            } else {
                System.out.println("Executed " + steps + " steps without hitting an unimplemented instruction.");
                System.out.println("(Reached the step budget rather than a real stopping condition - this");
                System.out.println(" interpreter found no stable repeating loop either; genuinely new code");
                System.out.println(" the whole way, or a loop longer than LoopDetector's repeat threshold.)");
            }
        } catch (UnsupportedOperationException | IllegalStateException e) {
            System.out.println("Stopped after " + steps + " step(s) at PC=0x" + Integer.toHexString(cpu.pc) + ":");
            System.out.println("  " + e.getMessage());
            System.out.println("This is expected at this stage: real boot code needs SH-4 instructions,");
            System.out.println("hardware registers, or an exception/interrupt mechanism (TRAPA, MMU) this");
            System.out.println("interpreter doesn't implement yet. See docs/STATUS.md \"Not started yet\".");
        }
    }

    /**
     * Reports a {@link LoopDetector}-confirmed stable cycle: the loop's period and how many
     * consecutive repeats confirmed it, plus (PC, opcode) for every instruction in one full
     * period, reconstructed from the ring buffer {@code attemptMinimalBoot} kept alongside
     * stepping — {@link LoopDetector} itself tracks only PC-to-step-index mappings, not the
     * PC sequence in order, so it can't reconstruct the loop body on its own.
     */
    private static void printLoopDetected(LoopDetector loopDetector, int[] pcRing, int[] opcodeRing,
                                           int ringPushes, int ringCapacity, int steps) {
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
                System.out.println(String.format("    PC=0x%08X opcode=0x%04X", pcRing[index], opcodeRing[index]));
            }
        }

        System.out.println("This may be legitimate but slow work (e.g. a byte-at-a-time memset/.bss-clear");
        System.out.println("loop — see docs/STATUS.md) or a genuine spin-wait on hardware state this");
        System.out.println("interpreter doesn't model yet (VBlank, DMA, timers — none exist so far).");
        System.out.println("Stopped early rather than exhausting the step budget on repeated output.");
    }
}
