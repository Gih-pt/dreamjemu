package org.dreamjemu.gdrom;

/**
 * A single ISO9660 directory entry.
 *
 * @param identifier the file/directory name, including the ISO9660 ";1" version
 *                    suffix on files (e.g. {@code "1ST_READ.BIN;1"}); empty for the
 *                    special self (".") and parent ("..") entries
 * @param extentLba   logical block address (2048-byte logical sectors) where this
 *                     entry's data starts
 * @param dataLength  size of this entry's data, in bytes
 * @param isDirectory whether this entry is itself a directory
 */
public record Iso9660DirectoryRecord(
        String identifier,
        long extentLba,
        long dataLength,
        boolean isDirectory
) {
}
