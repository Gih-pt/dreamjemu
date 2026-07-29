package org.dreamjemu.gdrom;

import java.io.IOException;

/**
 * Reads a single sector's worth of bytes at a given LBA into a buffer.
 * {@link GdiImage#readSector}, {@link CueBinImage#readSector}, and
 * {@link CdiImage#readSector} all already match this shape via method
 * reference, without needing to implement this interface directly.
 *
 * Kept deliberately narrow (like core-system's {@code Bus}) so higher-level
 * readers — {@link LogicalSectorReader}, {@link Iso9660FileSystem} — can work
 * against any disc image format without depending on its concrete class.
 */
@FunctionalInterface
public interface SectorSource {
    void readSector(long lba, byte[] dest) throws IOException;
}
