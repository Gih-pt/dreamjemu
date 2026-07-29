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
 * <b>What this does NOT do (yet):</b> it does not run the game. It reads the
 * IP.BIN boot header and locates the named boot file in the disc's ISO9660
 * root directory (LBA + size) — but does not yet load that file's bytes into
 * RAM or jump the SH-4 there. See /docs/STATUS.md and /docs/ROADMAP.md.
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
        System.out.println("the parsed IP.BIN boot header, and locates the boot file it names within");
        System.out.println("the disc's ISO9660 root directory. Point it at a disc image you already");
        System.out.println("legally own - this project does not provide or link to any.");
        System.out.println();
        System.out.println("This does not run the game yet; see docs/STATUS.md for what's implemented.");
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
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            System.out.println("Could not locate the boot file: " + message);
        }
        System.out.println();
        System.out.println("(This tool locates the boot file but does not load or run it yet.");
        System.out.println(" See docs/STATUS.md and docs/ROADMAP.md for what's implemented so far.)");
    }
}
