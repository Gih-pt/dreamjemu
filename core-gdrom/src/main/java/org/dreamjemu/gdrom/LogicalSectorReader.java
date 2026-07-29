package org.dreamjemu.gdrom;

import java.io.IOException;

/**
 * Wraps a raw {@link SectorSource} (whatever sector size a track declares —
 * 2352-byte "raw" MODE1 sectors, or plain 2048-byte sectors) into a
 * normalized reader that always returns exactly 2048 bytes of logical user
 * data for a given LBA, relative to a track's own start.
 *
 * Both IP.BIN header extraction and ISO9660 filesystem parsing only care
 * about this filesystem-visible 2048-byte data, never the raw sector layout
 * — this class is the one place that distinction is handled, instead of
 * being duplicated in each consumer.
 */
public final class LogicalSectorReader implements SectorSource {

    /** ISO9660 (and IP.BIN) logical sector size, fixed by the standard. */
    public static final int LOGICAL_SECTOR_SIZE = 2048;

    /** Sync + header bytes preceding the 2048 bytes of user data in a raw 2352-byte MODE1 sector. */
    private static final int RAW_SECTOR_PREAMBLE = 16;

    private final SectorSource rawSource;
    private final long trackStartLba;
    private final int rawSectorSize;

    public LogicalSectorReader(SectorSource rawSource, long trackStartLba, int rawSectorSize) {
        this.rawSource = rawSource;
        this.trackStartLba = trackStartLba;
        this.rawSectorSize = rawSectorSize;
    }

    @Override
    public void readSector(long logicalLba, byte[] dest) throws IOException {
        if (dest.length < LOGICAL_SECTOR_SIZE) {
            throw new IllegalArgumentException(
                    "Destination buffer must be at least " + LOGICAL_SECTOR_SIZE + " bytes, got " + dest.length);
        }
        byte[] raw = new byte[rawSectorSize];
        rawSource.readSector(trackStartLba + logicalLba, raw);
        int offset = rawSectorSize >= 2352 ? RAW_SECTOR_PREAMBLE : 0;
        System.arraycopy(raw, offset, dest, 0, LOGICAL_SECTOR_SIZE);
    }
}
