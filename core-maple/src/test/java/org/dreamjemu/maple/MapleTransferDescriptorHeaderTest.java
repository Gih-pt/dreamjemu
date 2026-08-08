package org.dreamjemu.maple;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapleTransferDescriptorHeaderTest {

    @Test
    void encodeDecodeRoundTrips() {
        MapleTransferDescriptorHeader original =
                new MapleTransferDescriptorHeader(true, 3, true, 2, 0x0C800300);

        int transferInfo = original.encodeTransferInfo();
        int resultAddress = original.encodeResultAddress();
        MapleTransferDescriptorHeader decoded = MapleTransferDescriptorHeader.decode(transferInfo, resultAddress);

        assertEquals(original, decoded);
    }

    /**
     * The first worked example in §2.6.8: 4 command-table entries, one per
     * port, each a 4-byte (1-word) transmission, with port 3's entry
     * marked as the end of the command list.
     */
    @Test
    void decodesEveryEntryOfTheFirstOfficialWorkedExample() {
        assertHeader(0x00000000, false, 0, 1); // Port 0, 4-byte, not last
        assertHeader(0x00010000, false, 1, 1); // Port 1, 4-byte, not last
        assertHeader(0x00020000, false, 2, 1); // Port 2, 4-byte, not last
        assertHeader(0x80030000, true, 3, 1);  // Port 3, 4-byte, command list end
    }

    /**
     * The second worked example in §2.6.8: same shape, but every entry is
     * an 8-byte (2-word) transmission this time.
     */
    @Test
    void decodesEveryEntryOfTheSecondOfficialWorkedExample() {
        assertHeader(0x00000001, false, 0, 2); // Port 0, 8-byte, not last
        assertHeader(0x00010001, false, 1, 2); // Port 1, 8-byte, not last
        assertHeader(0x00020001, false, 2, 2); // Port 2, 8-byte, not last
        assertHeader(0x80030001, true, 3, 2);  // Port 3, 8-byte, command list end
    }

    private static void assertHeader(int transferInfoWord, boolean expectedLast, int expectedPort,
                                      int expectedWordCount) {
        MapleTransferDescriptorHeader decoded = MapleTransferDescriptorHeader.decode(transferInfoWord, 0);

        assertEquals(expectedLast, decoded.lastDescriptor());
        assertEquals(expectedPort, decoded.port());
        assertEquals(expectedWordCount, decoded.wordCount());

        // And re-encoding must reproduce the exact literal word.
        MapleTransferDescriptorHeader withoutGun =
                new MapleTransferDescriptorHeader(expectedLast, expectedPort, false, expectedWordCount, 0);
        assertEquals(transferInfoWord, withoutGun.encodeTransferInfo());
    }

    @Test
    void resultAddressMatchesTheOfficialWorkedExamplesReceptionAddresses() {
        // "Port 0, reception data storage address" = 0x0C800000, etc.
        MapleTransferDescriptorHeader header = MapleTransferDescriptorHeader.decode(0x00000000, 0x0C800000);

        assertEquals(0x0C800000, header.resultAddress());
    }

    @Test
    void gunBitIsBitNine() {
        MapleTransferDescriptorHeader header = new MapleTransferDescriptorHeader(false, 0, true, 1, 0);

        assertEquals(1 << 9, header.encodeTransferInfo());
    }

    @Test
    void nonGunEntriesLeaveBitNineClear() {
        MapleTransferDescriptorHeader header = MapleTransferDescriptorHeader.decode(0x80030001, 0);

        assertFalse(header.gun());
    }

    @Test
    void rejectsPortOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> new MapleTransferDescriptorHeader(false, 4, false, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new MapleTransferDescriptorHeader(false, -1, false, 1, 0));
    }

    @Test
    void rejectsWordCountOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> new MapleTransferDescriptorHeader(false, 0, false, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new MapleTransferDescriptorHeader(false, 0, false, 257, 0));
    }

    @Test
    void maximumWordCountRoundTrips() {
        MapleTransferDescriptorHeader header = new MapleTransferDescriptorHeader(false, 0, false, 256, 0);

        assertEquals(0xFF, header.encodeTransferInfo() & 0xFF);
        assertTrue(MapleTransferDescriptorHeader.decode(header.encodeTransferInfo(), 0).wordCount() == 256);
    }
}
