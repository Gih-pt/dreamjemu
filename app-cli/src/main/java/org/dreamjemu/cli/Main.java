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
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }

        Path imagePath = Path.of(args[0]);
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
        System.out.println("Usage: dreamjemu-cli <path-to-disc-image.gdi|.cue|.cdi>");
        System.out.println();
        System.out.println("Detects the disc image format, loads it, locates the data track, prints");
        System.out.println("the parsed IP.BIN boot header, locates the boot file it names within the");
        System.out.println("disc's ISO9660 root directory, loads its bytes into a real system bus at");
        System.out.println("the documented Dreamcast boot address, and steps the SH-4 from there until");
        System.out.println("it hits something not implemented yet. Point it at a disc image you already");
        System.out.println("legally own - this project does not provide or link to any.");
        System.out.println();
        System.out.println("This does not run a real game to completion yet; see docs/STATUS.md.");
    }

    private static OpenDataTrack openGdiDataTrack(Path path) throws IOException {
        GdiImage image = GdiImage.load(path);
        for (GdiTrack track : image.tracks()) {
            if (track.type() == GdiTrackType.DATA) {
                return new OpenDataTrack(new LogicalSectorReader(image::readSector, track.startLba(), track.sectorSize()), image);
            }
        }
        image.close();
        throw new IOException("No data track found in " + path);
    }

    private static OpenDataTrack openCueBinDataTrack(Path path) throws IOException {
        CueBinImage image = CueBinImage.load(path);
        for (CueTrack track : image.tracks()) {
            if (!track.mode().isAudio()) {
                return new OpenDataTrack(new LogicalSectorReader(image::readSector, track.startLba(), track.sectorSize()), image);
            }
        }
        image.close();
        throw new IOException("No data track found in " + path);
    }

    private static OpenDataTrack openCdiDataTrack(Path path) throws IOException {
        CdiImage image = CdiImage.load(path);
        for (CdiTrack track : image.tracks()) {
            if (track.mode() != CdiTrackMode.CDDA) {
                return new OpenDataTrack(new LogicalSectorReader(image::readSector, track.startLba(), track.sectorSize()), image);
            }
        }
        image.close();
        throw new IOException("No data track found in " + path);
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
        final int maxSteps = 100_000;
        int steps = 0;
        try {
            for (; steps < maxSteps; steps++) {
                cpu.step();
            }
            System.out.println("Executed " + steps + " steps without hitting an unimplemented instruction.");
            System.out.println("(Reached the step budget rather than a real stopping condition - this");
            System.out.println(" interpreter has no way yet to detect an intentional halt/idle loop.)");
        } catch (UnsupportedOperationException | IllegalStateException e) {
            System.out.println("Stopped after " + steps + " step(s) at PC=0x" + Integer.toHexString(cpu.pc) + ":");
            System.out.println("  " + e.getMessage());
            System.out.println("This is expected at this stage: real boot code needs SH-4 instructions,");
            System.out.println("hardware registers, or an exception/interrupt mechanism (TRAPA, MMU) this");
            System.out.println("interpreter doesn't implement yet. See docs/STATUS.md \"Not started yet\".");
        }
    }
}
