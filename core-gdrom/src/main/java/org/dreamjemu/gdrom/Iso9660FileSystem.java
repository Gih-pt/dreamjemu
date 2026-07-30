package org.dreamjemu.gdrom;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal ISO9660 filesystem reader: parses the Primary Volume Descriptor to
 * find the root directory, then lists/looks up files directly within it.
 *
 * Scoped to exactly what's needed for the next step of the project's
 * BIOS-free HLE boot sequence (see /docs/ROADMAP.md): locating the boot
 * file named in {@link IpBinHeader#bootFilename()} (typically
 * {@code 1ST_READ.BIN}) so its data can eventually be loaded into RAM and
 * the SH-4 jumped there. Dreamcast boot executables live directly in the
 * disc's root directory, so nested subdirectory traversal is intentionally
 * out of scope here — a real, if narrow, capability rather than a
 * half-finished general-purpose ISO9660 implementation.
 *
 * This only reads whatever's on the user's own disc image via the
 * {@link SectorSource} it's given; it never requires or reads any original
 * console/BIOS file. Locating and reading the boot file's actual bytes
 * (loading it into RAM) is a follow-up step, not implemented here — this
 * class only resolves a file name to its extent (LBA + size).
 */
public final class Iso9660FileSystem {

    private static final long PRIMARY_VOLUME_DESCRIPTOR_LBA = 16;
    private static final int PVD_TYPE_CODE = 1;
    private static final int STANDARD_IDENTIFIER_OFFSET = 1;
    private static final String STANDARD_IDENTIFIER = "CD001";
    private static final int ROOT_DIRECTORY_RECORD_OFFSET = 156;

    private final SectorSource sectors;
    private final Iso9660DirectoryRecord rootDirectory;

    private Iso9660FileSystem(SectorSource sectors, Iso9660DirectoryRecord rootDirectory) {
        this.sectors = sectors;
        this.rootDirectory = rootDirectory;
    }

    /**
     * Opens an ISO9660 filesystem by reading and validating the Primary
     * Volume Descriptor at LBA 16 (a fixed offset defined by the standard),
     * and extracting its embedded root directory record.
     *
     * @param sectors a reader returning normalized 2048-byte logical sectors
     *                (see {@link LogicalSectorReader}) — NOT raw disc sectors
     * @throws IOException if the sector at LBA 16 isn't a valid Primary Volume Descriptor
     */
    public static Iso9660FileSystem open(SectorSource sectors) throws IOException {
        byte[] pvd = new byte[LogicalSectorReader.LOGICAL_SECTOR_SIZE];
        sectors.readSector(PRIMARY_VOLUME_DESCRIPTOR_LBA, pvd);

        int typeCode = pvd[0] & 0xFF;
        String standardId = new String(pvd, STANDARD_IDENTIFIER_OFFSET, 5, StandardCharsets.US_ASCII);
        if (!STANDARD_IDENTIFIER.equals(standardId) || typeCode != PVD_TYPE_CODE) {
            throw new IOException("Not a valid ISO9660 Primary Volume Descriptor at LBA " +
                    PRIMARY_VOLUME_DESCRIPTOR_LBA + " (type=" + typeCode + ", standard identifier=\"" + standardId + "\")");
        }

        Iso9660DirectoryRecord root = parseDirectoryRecord(pvd, ROOT_DIRECTORY_RECORD_OFFSET);
        return new Iso9660FileSystem(sectors, root);
    }

    public Iso9660DirectoryRecord rootDirectory() {
        return rootDirectory;
    }

    /** Lists every entry directly in the root directory (not recursive; excludes the "." and ".." entries). */
    public List<Iso9660DirectoryRecord> listRootDirectory() throws IOException {
        List<Iso9660DirectoryRecord> entries = new ArrayList<>();
        long sectorCount = (rootDirectory.dataLength() + LogicalSectorReader.LOGICAL_SECTOR_SIZE - 1)
                / LogicalSectorReader.LOGICAL_SECTOR_SIZE;

        for (long i = 0; i < sectorCount; i++) {
            byte[] sector = new byte[LogicalSectorReader.LOGICAL_SECTOR_SIZE];
            sectors.readSector(rootDirectory.extentLba() + i, sector);

            int offset = 0;
            while (offset < LogicalSectorReader.LOGICAL_SECTOR_SIZE) {
                int recordLength = sector[offset] & 0xFF;
                if (recordLength == 0) {
                    break; // padding to the end of this sector — no more records here
                }
                Iso9660DirectoryRecord record = parseDirectoryRecord(sector, offset);
                if (!record.identifier().isEmpty()) {
                    entries.add(record);
                }
                offset += recordLength;
            }
        }
        return entries;
    }

    /**
     * Finds a file directly in the root directory by name. Dreamcast boot
     * filenames from {@link IpBinHeader#bootFilename()} (e.g. {@code "1ST_READ.BIN"})
     * don't include the ISO9660 {@code ";1"} version suffix that directory
     * entries normally have, so the comparison strips that suffix before matching.
     *
     * @throws IOException if no matching file is found
     */
    public Iso9660DirectoryRecord findInRootDirectory(String fileName) throws IOException {
        for (Iso9660DirectoryRecord entry : listRootDirectory()) {
            if (!entry.isDirectory() && stripVersionSuffix(entry.identifier()).equalsIgnoreCase(fileName)) {
                return entry;
            }
        }
        throw new IOException("File not found in root directory: \"" + fileName + "\"");
    }

    /**
     * Reads a file's entire contents given its directory entry (as returned
     * by {@link #findInRootDirectory}) — the actual disc-reading half of
     * loading a Dreamcast boot executable into RAM. Writing those bytes into
     * a system's memory bus and pointing the SH-4 at them is deliberately
     * out of scope here (that's core-system/core-cpu-sh4's job, not
     * core-gdrom's) — this only turns a resolved (LBA, size) pair into the
     * actual bytes at that location.
     *
     * @param file a record previously returned by this same filesystem instance
     *             (its extent must be valid against this filesystem's {@link SectorSource})
     */
    public byte[] readFile(Iso9660DirectoryRecord file) throws IOException {
        int length = Math.toIntExact(file.dataLength());
        byte[] out = new byte[length];
        long sectorCount = (file.dataLength() + LogicalSectorReader.LOGICAL_SECTOR_SIZE - 1)
                / LogicalSectorReader.LOGICAL_SECTOR_SIZE;
        byte[] sectorBuf = new byte[LogicalSectorReader.LOGICAL_SECTOR_SIZE];

        for (long i = 0; i < sectorCount; i++) {
            sectors.readSector(file.extentLba() + i, sectorBuf);
            int destOffset = (int) (i * LogicalSectorReader.LOGICAL_SECTOR_SIZE);
            int copyLength = Math.min(LogicalSectorReader.LOGICAL_SECTOR_SIZE, length - destOffset);
            System.arraycopy(sectorBuf, 0, out, destOffset, copyLength);
        }
        return out;
    }

    private static String stripVersionSuffix(String identifier) {
        int semicolon = identifier.indexOf(';');
        return semicolon >= 0 ? identifier.substring(0, semicolon) : identifier;
    }

    private static Iso9660DirectoryRecord parseDirectoryRecord(byte[] data, int offset) {
        long extentLba = readUInt32LittleEndianHalf(data, offset + 2);
        long dataLength = readUInt32LittleEndianHalf(data, offset + 10);
        int fileFlags = data[offset + 25] & 0xFF;
        boolean isDirectory = (fileFlags & 0x02) != 0;
        int identifierLength = data[offset + 32] & 0xFF;

        String identifier;
        if (identifierLength == 1 && (data[offset + 33] == 0x00 || data[offset + 33] == 0x01)) {
            identifier = ""; // the "." (0x00) or ".." (0x01) special entries — not a real name
        } else {
            identifier = new String(data, offset + 33, identifierLength, StandardCharsets.US_ASCII);
        }

        return new Iso9660DirectoryRecord(identifier, extentLba, dataLength, isDirectory);
    }

    /** ISO9660 stores 32-bit numbers "both-endian" (little-endian half, then big-endian half); only the little-endian half is needed. */
    private static long readUInt32LittleEndianHalf(byte[] data, int offset) {
        return (data[offset] & 0xFFL)
                | ((data[offset + 1] & 0xFFL) << 8)
                | ((data[offset + 2] & 0xFFL) << 16)
                | ((data[offset + 3] & 0xFFL) << 24);
    }
}
