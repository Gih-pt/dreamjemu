package org.dreamjemu.gdrom;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IpBinHeaderTest {

    /**
     * Builds a synthetic 256-byte IP.BIN header with entirely fictional test
     * values (not extracted from any real game disc), space-padded exactly
     * like a real header, for testing the parser's field extraction.
     */
    private static byte[] buildSyntheticHeader() {
        byte[] header = new byte[IpBinHeader.HEADER_SIZE];
        Arrays.fill(header, (byte) ' ');

        place(header, 0x00, "SEGA SEGAKATANA");
        place(header, 0x10, "TEST MAKER");
        place(header, 0x20, "ABCD GD-ROM1/1");
        place(header, 0x30, "JUE");
        place(header, 0x38, "0000000");
        place(header, 0x40, "T-00001");
        place(header, 0x4A, "V1.000");
        place(header, 0x50, "20260101");
        place(header, 0x60, "1ST_READ.BIN");
        place(header, 0x70, "TEST STUDIO");
        place(header, 0x80, "TEST GAME TITLE");

        return header;
    }

    private static void place(byte[] header, int offset, String text) {
        byte[] bytes = text.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(bytes, 0, header, offset, bytes.length);
    }

    @Test
    void parsesAllFieldsCorrectlyAndTrimsPadding() {
        IpBinHeader ipBin = IpBinHeader.parse(buildSyntheticHeader());

        assertEquals("SEGA SEGAKATANA", ipBin.hardwareId());
        assertEquals("TEST MAKER", ipBin.makerId());
        assertEquals("ABCD GD-ROM1/1", ipBin.deviceInfo());
        assertEquals("JUE", ipBin.areaSymbols());
        assertEquals("0000000", ipBin.peripherals());
        assertEquals("T-00001", ipBin.productNumber());
        assertEquals("V1.000", ipBin.productVersion());
        assertEquals("20260101", ipBin.releaseDate());
        assertEquals("1ST_READ.BIN", ipBin.bootFilename());
        assertEquals("TEST STUDIO", ipBin.softwareCompany());
        assertEquals("TEST GAME TITLE", ipBin.softwareName());
    }

    @Test
    void recognizesAValidDreamcastHardwareId() {
        IpBinHeader ipBin = IpBinHeader.parse(buildSyntheticHeader());

        assertTrue(ipBin.isValidDreamcastHeader());
    }

    @Test
    void rejectsAnInvalidHardwareId() {
        byte[] header = buildSyntheticHeader();
        place(header, 0x00, "NOT A DREAMCAST "); // overwrite with garbage, still 16 bytes or fewer

        IpBinHeader ipBin = IpBinHeader.parse(header);

        assertFalse(ipBin.isValidDreamcastHeader());
    }

    @Test
    void tooShortHeaderThrows() {
        byte[] tooShort = new byte[100];

        assertThrows(IllegalArgumentException.class, () -> IpBinHeader.parse(tooShort));
    }

    @Test
    void fieldsWithNoTrailingPaddingAreUnaffected() {
        // Product version (6 bytes) and release date (8 bytes) are exactly
        // filled with no padding in the synthetic fixture -- confirms
        // trimming doesn't eat real content when there's nothing to trim.
        IpBinHeader ipBin = IpBinHeader.parse(buildSyntheticHeader());

        assertEquals(6, ipBin.productVersion().length());
        assertEquals(8, ipBin.releaseDate().length());
    }

    @Test
    void crc16MatchesTheStandardCcittFalseCheckValue() {
        // No independently-sourced real Dreamcast product-number/version-to-CRC
        // example was available, so this verifies the algorithm itself against
        // the standard, independently-published check value for the CRC-16
        // variant it turns out to be (CRC-16/CCITT-FALSE): 0x29B1 for "123456789".
        byte[] input = "123456789".getBytes(StandardCharsets.US_ASCII);

        int crc = IpBinHeader.crc16Ccitt(input, 0, input.length);

        assertEquals(0x29B1, crc);
    }

    @Test
    void calculateDeviceInfoCrcMatchesEmbeddedCrcInSyntheticHeader() {
        byte[] header = buildSyntheticHeader();
        // buildSyntheticHeader() already places "T-00001" / "V1.000" at the
        // Product Number / Product Version offsets (0x40/0x4A) -- compute what
        // their real checksum is, and write it into the Device Info field's
        // 4-hex-digit CRC prefix, exactly where a genuine disc would have it.
        int expectedCrc = IpBinHeader.calculateDeviceInfoCrc(header);
        place(header, 0x20, String.format("%04X GD-ROM1/1", expectedCrc));

        IpBinHeader ipBin = IpBinHeader.parse(header);

        assertEquals(java.util.OptionalInt.of(expectedCrc), ipBin.embeddedDeviceInfoCrc());
    }

    @Test
    void embeddedDeviceInfoCrcIsEmptyWhenNotHexDigits() {
        byte[] header = buildSyntheticHeader(); // deviceInfo starts with "ABCD", which IS valid hex...
        place(header, 0x20, "ZZZZ GD-ROM1/1"); // ...so overwrite with non-hex characters instead

        IpBinHeader ipBin = IpBinHeader.parse(header);

        assertEquals(java.util.OptionalInt.empty(), ipBin.embeddedDeviceInfoCrc());
    }
}
