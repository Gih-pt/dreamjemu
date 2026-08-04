package org.dreamjemu.maple;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MapleDeviceInfoTest {

    private static MapleDeviceInfo sampleController() {
        return new MapleDeviceInfo(
                MapleFunctionCode.CONTROLLER,
                new int[] {0xFE060F00, 0, 0},
                (byte) 0xFF,
                (byte) 0,
                "Dreamcast Controller",
                "Produced By or Under License From SEGA ENTERPRISES,LTD.",
                (short) 0x00C0,
                (short) 0x01F4);
    }

    @Test
    void encodedLengthIs112Bytes() {
        assertEquals(112, sampleController().encode().length);
        assertEquals(112, MapleDeviceInfo.ENCODED_LENGTH);
    }

    @Test
    void encodeDecodeRoundTrips() {
        MapleDeviceInfo original = sampleController();

        byte[] encoded = original.encode();
        MapleDeviceInfo decoded = MapleDeviceInfo.decode(encoded, 0);

        assertEquals(original.functionCodes(), decoded.functionCodes());
        assertArrayEquals(original.functionData(), decoded.functionData());
        assertEquals(original.areaCode(), decoded.areaCode());
        assertEquals(original.connectorDirection(), decoded.connectorDirection());
        assertEquals(original.productName(), decoded.productName());
        assertEquals(original.productLicense(), decoded.productLicense());
        assertEquals(original.standbyPower(), decoded.standbyPower());
        assertEquals(original.maxPower(), decoded.maxPower());
    }

    @Test
    void functionCodesAndFunctionDataAreBigEndian() {
        MapleDeviceInfo info = new MapleDeviceInfo(
                0x00000001, new int[] {0x11223344, 0, 0}, (byte) 0, (byte) 0, "", "", (short) 0, (short) 0);

        byte[] encoded = info.encode();

        assertEquals(0x00, encoded[0] & 0xFF);
        assertEquals(0x00, encoded[1] & 0xFF);
        assertEquals(0x00, encoded[2] & 0xFF);
        assertEquals(0x01, encoded[3] & 0xFF);
        assertEquals(0x11, encoded[4] & 0xFF);
        assertEquals(0x22, encoded[5] & 0xFF);
        assertEquals(0x33, encoded[6] & 0xFF);
        assertEquals(0x44, encoded[7] & 0xFF);
    }

    @Test
    void standbyAndMaxPowerAreLittleEndian() {
        MapleDeviceInfo info = new MapleDeviceInfo(
                0, new int[] {0, 0, 0}, (byte) 0, (byte) 0, "", "", (short) 0x00C0, (short) 0x01F4);

        byte[] encoded = info.encode();
        int standbyOffset = 4 + 12 + 1 + 1 + 30 + 60;

        assertEquals(0xC0, encoded[standbyOffset] & 0xFF);
        assertEquals(0x00, encoded[standbyOffset + 1] & 0xFF);
        assertEquals(0xF4, encoded[standbyOffset + 2] & 0xFF);
        assertEquals(0x01, encoded[standbyOffset + 3] & 0xFF);
    }

    @Test
    void productNameAndLicenseAreNulPaddedAndTrimmedOnDecode() {
        MapleDeviceInfo info = new MapleDeviceInfo(
                0, new int[] {0, 0, 0}, (byte) 0, (byte) 0, "short", "", (short) 0, (short) 0);

        byte[] encoded = info.encode();
        int nameOffset = 4 + 12 + 1 + 1;
        // Every byte after "short" (5 chars) up to the 30-byte field width is NUL padding.
        for (int i = 5; i < 30; i++) {
            assertEquals(0, encoded[nameOffset + i]);
        }

        MapleDeviceInfo decoded = MapleDeviceInfo.decode(encoded, 0);
        assertEquals("short", decoded.productName());
        assertEquals("", decoded.productLicense());
    }

    @Test
    void decodeAlsoTrimsSpacePadding() {
        byte[] encoded = new byte[MapleDeviceInfo.ENCODED_LENGTH];
        int nameOffset = 4 + 12 + 1 + 1;
        byte[] nameBytes = "PADDED".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        System.arraycopy(nameBytes, 0, encoded, nameOffset, nameBytes.length);
        for (int i = nameBytes.length; i < 30; i++) {
            encoded[nameOffset + i] = ' ';
        }

        MapleDeviceInfo decoded = MapleDeviceInfo.decode(encoded, 0);

        assertEquals("PADDED", decoded.productName());
    }

    @Test
    void rejectsOversizedProductNameOrLicense() {
        String tooLong31 = "1234567890123456789012345678901"; // 31 chars, field is 30
        assertThrows(IllegalArgumentException.class,
                () -> new MapleDeviceInfo(0, new int[] {0, 0, 0}, (byte) 0, (byte) 0, tooLong31, "", (short) 0, (short) 0));
    }

    @Test
    void rejectsFunctionDataOfWrongLength() {
        assertThrows(IllegalArgumentException.class,
                () -> new MapleDeviceInfo(0, new int[] {0, 0}, (byte) 0, (byte) 0, "", "", (short) 0, (short) 0));
    }

    @Test
    void decodeAtNonZeroOffsetWorksAndDoesNotReadBeforeOffset() {
        MapleDeviceInfo original = sampleController();
        byte[] encoded = original.encode();
        byte[] withPrefix = new byte[4 + encoded.length];
        withPrefix[0] = (byte) 0xAA;
        withPrefix[1] = (byte) 0xBB;
        withPrefix[2] = (byte) 0xCC;
        withPrefix[3] = (byte) 0xDD;
        System.arraycopy(encoded, 0, withPrefix, 4, encoded.length);

        MapleDeviceInfo decoded = MapleDeviceInfo.decode(withPrefix, 4);

        assertEquals(original.functionCodes(), decoded.functionCodes());
        assertEquals(original.productName(), decoded.productName());
    }

    @Test
    void decodeThrowsWhenNotEnoughDataRemains() {
        byte[] tooShort = new byte[100];
        assertThrows(IllegalArgumentException.class, () -> MapleDeviceInfo.decode(tooShort, 0));
    }

    @Test
    void equalsComparesFunctionDataByValueNotByArrayReference() {
        // Two separately-constructed arrays with the same contents: a plain
        // record's default equals would consider these unequal (int[]
        // equals is reference-based) - confirms the explicit override works.
        MapleDeviceInfo a = new MapleDeviceInfo(
                1, new int[] {1, 2, 3}, (byte) 0, (byte) 0, "x", "y", (short) 1, (short) 2);
        MapleDeviceInfo b = new MapleDeviceInfo(
                1, new int[] {1, 2, 3}, (byte) 0, (byte) 0, "x", "y", (short) 1, (short) 2);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void functionDataAccessorReturnsADefensiveCopy() {
        int[] source = {1, 2, 3};
        MapleDeviceInfo info = new MapleDeviceInfo(
                0, source, (byte) 0, (byte) 0, "", "", (short) 0, (short) 0);
        source[0] = 99; // mutating the caller's original array afterward...

        assertEquals(1, info.functionData()[0]); // ...must not affect the stored copy.

        int[] returned = info.functionData();
        returned[0] = 42; // mutating the returned array...
        assertEquals(1, info.functionData()[0]); // ...must not affect internal state either.
    }
}
