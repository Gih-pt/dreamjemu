package org.dreamjemu.gdrom;

import java.nio.charset.StandardCharsets;

/**
 * Parses the first 256 bytes of a Dreamcast disc's IP.BIN — the descriptive
 * header at the very start of the data track, identifying the disc and
 * naming the boot executable (typically {@code 1ST_READ.BIN}) that a real
 * BIOS would load and jump to.
 *
 * This is the first piece of the project's BIOS-free HLE boot sequence (see
 * /docs/ROADMAP.md): reading and understanding this header is a prerequisite
 * for eventually locating the boot file in the disc's ISO9660 filesystem,
 * loading it into RAM, and jumping the SH-4 there — none of which requires
 * or reads any original Sega BIOS/firmware file. IP.BIN itself lives on the
 * user's own legally-owned disc image, not extracted from console hardware.
 *
 * <b>On the field layout below:</b> this project does not have access to an
 * original Sega technical reference for this header, and a direct fetch of
 * an existing open-source emulator's source file was blocked by that site's
 * robots.txt during development. The layout implemented here was instead
 * reconstructed by cross-referencing multiple independent public sources:
 * real debug log output from three different retail games (via Flycast's
 * open-source "reios" HLE BIOS, itself GPL-licensed and built from the same
 * kind of publicly available technical knowledge), and a separately
 * documented, independently confirmed byte offset for the region-flag byte
 * (0x30) used by an unrelated IP.BIN-patching tool. The resulting field
 * widths also sum to exactly 0x80 (128) bytes before the game title field,
 * which is a strong internal consistency signal for a real hardware format.
 * Even so, this has NOT been validated against a real disc image byte-for-
 * byte, and should be treated as provisional until a contributor does that
 * validation (see /docs/STATUS.md) — this is a case where an incorrect
 * offset would fail loudly (garbled/empty fields) rather than silently.
 */
public record IpBinHeader(
        String hardwareId,
        String makerId,
        String deviceInfo,
        String areaSymbols,
        String peripherals,
        String productNumber,
        String productVersion,
        String releaseDate,
        String bootFilename,
        String softwareCompany,
        String softwareName
) {

    /** Total header size in bytes (offsets 0x00-0xFF). */
    public static final int HEADER_SIZE = 256;

    /** Expected value of {@link #hardwareId()} on a genuine Dreamcast disc. */
    public static final String EXPECTED_HARDWARE_ID = "SEGA SEGAKATANA";

    private static final int OFF_HARDWARE_ID = 0x00;
    private static final int LEN_HARDWARE_ID = 16;

    private static final int OFF_MAKER_ID = 0x10;
    private static final int LEN_MAKER_ID = 16;

    private static final int OFF_DEVICE_INFO = 0x20;
    private static final int LEN_DEVICE_INFO = 16;

    private static final int OFF_AREA_SYMBOLS = 0x30;
    private static final int LEN_AREA_SYMBOLS = 8;

    private static final int OFF_PERIPHERALS = 0x38;
    private static final int LEN_PERIPHERALS = 8;

    private static final int OFF_PRODUCT_NUMBER = 0x40;
    private static final int LEN_PRODUCT_NUMBER = 10;

    private static final int OFF_PRODUCT_VERSION = 0x4A;
    private static final int LEN_PRODUCT_VERSION = 6;

    private static final int OFF_RELEASE_DATE = 0x50;
    private static final int LEN_RELEASE_DATE = 8;

    // 0x58-0x5F (8 bytes): spare/reserved, intentionally skipped.

    private static final int OFF_BOOT_FILENAME = 0x60;
    private static final int LEN_BOOT_FILENAME = 16;

    private static final int OFF_SOFTWARE_COMPANY = 0x70;
    private static final int LEN_SOFTWARE_COMPANY = 16;

    private static final int OFF_SOFTWARE_NAME = 0x80;
    private static final int LEN_SOFTWARE_NAME = 128;

    /**
     * Parses an IP.BIN header from a byte array of at least {@link #HEADER_SIZE} bytes.
     *
     * @throws IllegalArgumentException if the array is too short
     */
    public static IpBinHeader parse(byte[] header) {
        if (header.length < HEADER_SIZE) {
            throw new IllegalArgumentException(
                    "IP.BIN header must be at least " + HEADER_SIZE + " bytes, got " + header.length);
        }
        return new IpBinHeader(
                field(header, OFF_HARDWARE_ID, LEN_HARDWARE_ID),
                field(header, OFF_MAKER_ID, LEN_MAKER_ID),
                field(header, OFF_DEVICE_INFO, LEN_DEVICE_INFO),
                field(header, OFF_AREA_SYMBOLS, LEN_AREA_SYMBOLS),
                field(header, OFF_PERIPHERALS, LEN_PERIPHERALS),
                field(header, OFF_PRODUCT_NUMBER, LEN_PRODUCT_NUMBER),
                field(header, OFF_PRODUCT_VERSION, LEN_PRODUCT_VERSION),
                field(header, OFF_RELEASE_DATE, LEN_RELEASE_DATE),
                field(header, OFF_BOOT_FILENAME, LEN_BOOT_FILENAME),
                field(header, OFF_SOFTWARE_COMPANY, LEN_SOFTWARE_COMPANY),
                field(header, OFF_SOFTWARE_NAME, LEN_SOFTWARE_NAME)
        );
    }

    private static String field(byte[] header, int offset, int length) {
        String raw = new String(header, offset, length, StandardCharsets.US_ASCII);
        // Fields are space-padded (occasionally null-padded); trim both.
        int end = raw.length();
        while (end > 0 && (raw.charAt(end - 1) == ' ' || raw.charAt(end - 1) == '\0')) {
            end--;
        }
        return raw.substring(0, end);
    }

    /** True if {@link #hardwareId()} matches the expected Dreamcast hardware ID string. */
    public boolean isValidDreamcastHeader() {
        return EXPECTED_HARDWARE_ID.equals(hardwareId());
    }
}
