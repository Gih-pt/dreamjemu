package org.dreamjemu.gdrom;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Iso9660FileSystemTest {

    private static final int SECTOR_SIZE = LogicalSectorReader.LOGICAL_SECTOR_SIZE;

    /** A simple in-memory SectorSource backed by a flat array of logical sectors, for testing. */
    private static final class MemorySectorSource implements SectorSource {
        private final byte[][] sectors;

        MemorySectorSource(int sectorCount) {
            sectors = new byte[sectorCount][SECTOR_SIZE];
        }

        byte[] sector(int lba) {
            return sectors[lba];
        }

        @Override
        public void readSector(long lba, byte[] dest) {
            System.arraycopy(sectors[(int) lba], 0, dest, 0, SECTOR_SIZE);
        }
    }

    private static void writeUInt32BothEndian(byte[] data, int offset, long value) {
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
        data[offset + 2] = (byte) ((value >> 16) & 0xFF);
        data[offset + 3] = (byte) ((value >> 24) & 0xFF);
        data[offset + 4] = (byte) ((value >> 24) & 0xFF);
        data[offset + 5] = (byte) ((value >> 16) & 0xFF);
        data[offset + 6] = (byte) ((value >> 8) & 0xFF);
        data[offset + 7] = (byte) (value & 0xFF);
    }

    /** Writes one directory record at the given offset and returns its length (so callers can advance). */
    private static int writeDirectoryRecord(byte[] sector, int offset, long extentLba, long dataLength,
                                             int fileFlags, String identifier) {
        int idLen = identifier.length();
        int baseLen = 33 + idLen;
        int recordLen = (baseLen % 2 == 0) ? baseLen : baseLen + 1;

        sector[offset] = (byte) recordLen;
        sector[offset + 1] = 0; // extended attribute length
        writeUInt32BothEndian(sector, offset + 2, extentLba);
        writeUInt32BothEndian(sector, offset + 10, dataLength);
        // offset+18..+24 (date/time) left as zero — not needed for this project's purposes
        sector[offset + 25] = (byte) fileFlags;
        sector[offset + 26] = 0; // file unit size
        sector[offset + 27] = 0; // interleave gap
        writeUInt32BothEndian(sector, offset + 28, 1); // volume sequence number
        sector[offset + 32] = (byte) idLen;
        byte[] idBytes = identifier.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(idBytes, 0, sector, offset + 33, idBytes.length);

        return recordLen;
    }

    /**
     * Builds a small synthetic ISO9660 volume: a valid Primary Volume
     * Descriptor at LBA 16 whose root directory record points at LBA 20,
     * and a root directory (at LBA 20) containing "." and ".." plus one
     * file entry, "1ST_READ.BIN;1", pointing at LBA 21 with a distinctive
     * size — entirely fictional test data.
     */
    private static MemorySectorSource buildSyntheticDisc() {
        MemorySectorSource source = new MemorySectorSource(24);

        byte[] pvd = source.sector(16);
        pvd[0] = 1; // Primary Volume Descriptor type
        byte[] stdId = "CD001".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(stdId, 0, pvd, 1, stdId.length);
        pvd[6] = 1; // version
        writeDirectoryRecord(pvd, 156, 20, SECTOR_SIZE, 0x02, "\0"); // root dir record: self, at LBA 20

        byte[] rootDir = source.sector(20);
        int offset = 0;
        offset += writeDirectoryRecord(rootDir, offset, 20, SECTOR_SIZE, 0x02, "\0");     // "."
        offset += writeDirectoryRecord(rootDir, offset, 20, SECTOR_SIZE, 0x02, "\u0001"); // ".."
        writeDirectoryRecord(rootDir, offset, 21, 12345, 0, "1ST_READ.BIN;1");

        return source;
    }

    @Test
    void opensAndParsesTheRootDirectoryRecord() throws IOException {
        Iso9660FileSystem fs = Iso9660FileSystem.open(buildSyntheticDisc());

        assertEquals(20, fs.rootDirectory().extentLba());
        assertTrue(fs.rootDirectory().isDirectory());
    }

    @Test
    void listsRootDirectoryEntriesExcludingSelfAndParent() throws IOException {
        Iso9660FileSystem fs = Iso9660FileSystem.open(buildSyntheticDisc());

        List<Iso9660DirectoryRecord> entries = fs.listRootDirectory();

        assertEquals(1, entries.size());
        assertEquals("1ST_READ.BIN;1", entries.get(0).identifier());
    }

    @Test
    void findsBootFileStrippingTheVersionSuffix() throws IOException {
        Iso9660FileSystem fs = Iso9660FileSystem.open(buildSyntheticDisc());

        Iso9660DirectoryRecord bootFile = fs.findInRootDirectory("1ST_READ.BIN");

        assertEquals(21, bootFile.extentLba());
        assertEquals(12345, bootFile.dataLength());
        assertFalse(bootFile.isDirectory());
    }

    @Test
    void missingFileThrowsClearError() throws IOException {
        Iso9660FileSystem fs = Iso9660FileSystem.open(buildSyntheticDisc());

        assertThrows(IOException.class, () -> fs.findInRootDirectory("DOES_NOT_EXIST.BIN"));
    }

    @Test
    void readsTheBootFilesFullContentsAcrossMultipleSectors() throws IOException {
        MemorySectorSource source = buildSyntheticDisc();

        // The boot file entry (from buildSyntheticDisc) declares 12345 bytes
        // starting at LBA 21 — that's more than one 2048-byte logical sector
        // (ceil(12345/2048) = 7 sectors), so this also exercises the
        // multi-sector / partial-last-sector path, not just a single read.
        // Fill LBA 21..27 with a distinctive, position-dependent byte pattern
        // (not all zero) so a wrong offset or truncated copy would produce a
        // detectably wrong result rather than accidentally passing.
        for (int lba = 21; lba <= 27; lba++) {
            byte[] sector = source.sector(lba);
            for (int i = 0; i < SECTOR_SIZE; i++) {
                sector[i] = (byte) (lba * 31 + i);
            }
        }

        Iso9660FileSystem fs = Iso9660FileSystem.open(source);
        Iso9660DirectoryRecord bootFile = fs.findInRootDirectory("1ST_READ.BIN");

        byte[] contents = fs.readFile(bootFile);

        assertEquals(12345, contents.length);
        // First byte: LBA 21, offset 0.
        assertEquals((byte) (21 * 31), contents[0]);
        // Last byte: falls in LBA 27 (12345 / 2048 = 6 sectors + a partial 7th),
        // at offset (12345 - 1) - 6*2048 within that final sector.
        int lastSectorOffset = (12345 - 1) - 6 * SECTOR_SIZE;
        assertEquals((byte) (27 * 31 + lastSectorOffset), contents[12345 - 1]);
    }

    @Test
    void rejectsANonIso9660Sector() {
        MemorySectorSource source = new MemorySectorSource(24); // LBA 16 stays all-zero — not a valid PVD

        assertThrows(IOException.class, () -> Iso9660FileSystem.open(source));
    }
}
