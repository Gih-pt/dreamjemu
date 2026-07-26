package org.dreamjemu.cli;

import org.dreamjemu.gdrom.CueBinImage;
import org.dreamjemu.gdrom.CueTrack;
import org.dreamjemu.gdrom.DiscImageDetector;
import org.dreamjemu.gdrom.DiscImageFormat;
import org.dreamjemu.gdrom.GdiImage;
import org.dreamjemu.gdrom.GdiTrack;
import org.dreamjemu.gdrom.GdiTrackType;
import org.dreamjemu.gdrom.IpBinHeader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A simple command-line tool for detecting, loading, and inspecting a
 * Dreamcast disc image — a lightweight, GUI-free way to sanity-check that
 * format detection and sector reading actually work against a real disc
 * image the user has locally, while app-javafx's UI is still early.
 *
 * <b>What this does NOT do (yet):</b> it does not run the game. Actually
 * booting requires parsing the disc's ISO9660 filesystem to locate the boot
 * file named in IP.BIN, loading it into RAM, and jumping the SH-4 there —
 * none of that exists yet (see /docs/STATUS.md and /docs/ROADMAP.md). This
 * tool only goes as far as reading and displaying the boot header.
 *
 * Usage: point it at a disc image file the user already legally owns —
 * this project does not provide, link to, or facilitate finding any disc
 * images itself; see README.md's stance on piracy.
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
            byte[] rawHeaderSector = switch (format) {
                case GDI -> readFirstDataSectorFromGdi(imagePath);
                case CUE_BIN -> readFirstDataSectorFromCueBin(imagePath);
                case CDI, CHD -> null;
                case UNKNOWN -> null;
            };

            if (rawHeaderSector == null) {
                System.out.println();
                System.out.println("Sector reading is not implemented yet for format: " + format);
                System.out.println("Only GDI and CUE/BIN reading exist so far — see docs/STATUS.md.");
                System.exit(2);
                return;
            }

            IpBinHeader ipBin = IpBinHeader.parse(extractIpBinHeaderBytes(rawHeaderSector));
            printIpBinHeader(ipBin);
        } catch (IOException e) {
            System.err.println("Failed to read disc image: " + e.getMessage());
            System.exit(3);
        }
    }

    private static void printUsage() {
        System.out.println("DreamJEmu disc image inspector (CLI)");
        System.out.println();
        System.out.println("Usage: dreamjemu-cli <path-to-disc-image.gdi|.cue>");
        System.out.println();
        System.out.println("Detects the disc image format, loads it, locates the data track, and");
        System.out.println("prints the parsed IP.BIN boot header. Point it at a disc image you");
        System.out.println("already legally own - this project does not provide or link to any.");
        System.out.println();
        System.out.println("This does not run the game yet; see docs/STATUS.md for what's implemented.");
    }

    @FunctionalInterface
    private interface SectorReader {
        void read(long lba, byte[] dest) throws IOException;
    }

    private static byte[] readFirstDataSectorFromGdi(Path path) throws IOException {
        try (GdiImage image = GdiImage.load(path)) {
            for (GdiTrack track : image.tracks()) {
                if (track.type() == GdiTrackType.DATA) {
                    return readSector(image::readSector, track.startLba(), track.sectorSize());
                }
            }
            throw new IOException("No data track found in " + path);
        }
    }

    private static byte[] readFirstDataSectorFromCueBin(Path path) throws IOException {
        try (CueBinImage image = CueBinImage.load(path)) {
            for (CueTrack track : image.tracks()) {
                if (!track.mode().isAudio()) {
                    return readSector(image::readSector, track.startLba(), track.sectorSize());
                }
            }
            throw new IOException("No data track found in " + path);
        }
    }

    private static byte[] readSector(SectorReader reader, long lba, int sectorSize) throws IOException {
        byte[] sector = new byte[sectorSize];
        reader.read(lba, sector);
        return sector;
    }

    /**
     * Extracts IP.BIN's 256-byte header from a raw sector. Sector data
     * layout depends on the track's declared sector size: a 2352-byte
     * "raw" sector (the common case for Dreamcast dumps) has a 16-byte
     * sync+header preamble before the actual 2048 bytes of user data;
     * a 2048-byte sector already IS the user data, with no preamble.
     */
    private static byte[] extractIpBinHeaderBytes(byte[] sector) throws IOException {
        int dataOffset = sector.length >= 2352 ? 16 : 0;
        if (sector.length - dataOffset < IpBinHeader.HEADER_SIZE) {
            throw new IOException("Sector too small to contain an IP.BIN header");
        }
        byte[] header = new byte[IpBinHeader.HEADER_SIZE];
        System.arraycopy(sector, dataOffset, header, 0, IpBinHeader.HEADER_SIZE);
        return header;
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
        System.out.println();
        System.out.println("(This tool only inspects the boot header - it does not run the game yet.");
        System.out.println(" See docs/STATUS.md and docs/ROADMAP.md for what's implemented so far.)");
    }
}
