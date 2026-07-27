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
 * <b>On the field layout below:</b> every offset was cross-checked
 * field-by-field against Marcus Comstedt's "Dreamcast Programming -
 * IP0000.BIN" page (mc.pp.se/dc/ip0000.bin.html) — the original, most-cited
 * public technical reference for this header, in continuous use by the
 * Dreamcast homebrew/emulation community since 2000 (and the ultimate
 * source, directly or indirectly, for essentially every open-source tool
 * that reads IP.BIN, including Flycast's "reios"). Every field below
 * matches that reference exactly; nothing here was extracted from Sega's
 * own materials or any BIOS/firmware dump. The 16-byte Device Information
 * field's structure and its embedded CRC-16 algorithm (see
 * {@link #calculateDeviceInfoCrc}) are documented there too, and that
 * algorithm's implementation here has been independently verified against
 * the standard CRC-16/CCITT-FALSE check value (see
 * {@code IpBinHeaderTest}) — the variant it turns out to be — since no
 * publicly available, independently-sourced real Dreamcast product-number/
 * version-to-CRC example was found to cross-check against directly.
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

    /**
     * Parses the 4-hex-digit CRC-16 prefix embedded at the start of
     * {@link #deviceInfo()} (e.g. {@code "8B40"} in {@code "8B40 GD-ROM2/3"}),
     * for comparing against {@link #calculateDeviceInfoCrc}.
     *
     * @return the parsed value, or empty if the field doesn't start with 4 hex digits
     */
    public java.util.OptionalInt embeddedDeviceInfoCrc() {
        if (deviceInfo.length() < 4) {
            return java.util.OptionalInt.empty();
        }
        try {
            return java.util.OptionalInt.of(Integer.parseInt(deviceInfo.substring(0, 4), 16));
        } catch (NumberFormatException e) {
            return java.util.OptionalInt.empty();
        }
    }

    /**
     * Computes the CRC-16 that a genuine disc's {@link #deviceInfo()} field
     * should begin with: a checksum over the raw Product Number + Product
     * Version bytes (offsets 0x40-0x4F, 16 bytes total — <i>before</i>
     * padding is trimmed, since padding spaces are part of the checksummed
     * data). Per mc.pp.se/dc/ip0000.bin.html, this is "exactly the same CRC
     * algorithm as for the VMS file headers, except that the initial
     * remainder is FFFF instead of 0" — which turns out to be the standard
     * CRC-16/CCITT-FALSE variant (poly 0x1021, init 0xFFFF, no bit
     * reflection, no output XOR); see {@link #crc16Ccitt} for where that's
     * verified against an independently-published check value.
     *
     * @param header the full 256-byte (or longer) raw header, as passed to {@link #parse}
     */
    public static int calculateDeviceInfoCrc(byte[] header) {
        return crc16Ccitt(header, OFF_PRODUCT_NUMBER, LEN_PRODUCT_NUMBER + LEN_PRODUCT_VERSION);
    }

    /**
     * CRC-16/CCITT-FALSE (poly 0x1021, init 0xFFFF, no reflection, no XOR-out) —
     * a direct port of the C implementation documented at
     * mc.pp.se/dc/ip0000.bin.html for IP.BIN's Device Information field.
     * Package-private so {@code IpBinHeaderTest} can verify it directly
     * against that variant's standard, independently-published check value
     * (0x29B1 for the ASCII string {@code "123456789"}) — since no
     * independently-sourced real Dreamcast product-number/version-to-CRC
     * example was available to cross-check the higher-level
     * {@link #calculateDeviceInfoCrc} against directly.
     */
    static int crc16Ccitt(byte[] data, int offset, int length) {
        int n = 0xFFFF;
        for (int i = offset; i < offset + length; i++) {
            n ^= (data[i] & 0xFF) << 8;
            for (int bit = 0; bit < 8; bit++) {
                n = ((n & 0x8000) != 0) ? ((n << 1) ^ 4129) : (n << 1);
            }
        }
        return n & 0xFFFF;
    }
}
