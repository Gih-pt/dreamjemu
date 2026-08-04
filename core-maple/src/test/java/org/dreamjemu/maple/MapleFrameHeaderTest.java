package org.dreamjemu.maple;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MapleFrameHeaderTest {

    @Test
    void encodesRequestDeviceInfoFromHostToPortAMainPeripheral() {
        // Host (port A) -> port A main peripheral, REQUEST_DEVICE_INFO, no extra words.
        int recipient = MapleAddress.encode(0, true, 0);
        int sender = MapleAddress.host(0);
        MapleFrameHeader header = new MapleFrameHeader(MapleCommand.REQUEST_DEVICE_INFO, recipient, sender, 0);

        int encoded = header.encode();

        assertEquals(0x01, (encoded >>> 24) & 0xFF);
        assertEquals(recipient, (encoded >>> 16) & 0xFF);
        assertEquals(sender, (encoded >>> 8) & 0xFF);
        assertEquals(0, encoded & 0xFF);
    }

    @Test
    void encodeDecodeRoundTrips() {
        int recipient = MapleAddress.encode(2, true, 0b00101);
        int sender = MapleAddress.host(2);
        MapleFrameHeader original = new MapleFrameHeader(MapleCommand.GET_CONDITION, recipient, sender, 1);

        MapleFrameHeader decoded = MapleFrameHeader.decode(original.encode());

        assertEquals(original, decoded);
    }

    @Test
    void decodesNegativeResponseCodeCorrectly() {
        // FUNCTION_CODE_UNSUPPORTED (-2) in the top byte, arbitrary addresses/count.
        int word = (0xFE << 24) | (0x20 << 16) | (0x00 << 8) | 0x00;
        MapleFrameHeader header = MapleFrameHeader.decode(word);

        assertEquals(MapleCommand.FUNCTION_CODE_UNSUPPORTED, header.command());
        assertEquals(0x20, header.recipientAddress());
        assertEquals(0x00, header.senderAddress());
        assertEquals(0, header.additionalWordCount());
    }

    @Test
    void encodeBytesMatchesEncodeAsBigEndianBytes() {
        MapleFrameHeader header = new MapleFrameHeader(MapleCommand.GET_CONDITION, 0x21, 0x00, 1);

        byte[] bytes = header.encodeBytes();
        int word = header.encode();

        assertEquals(4, bytes.length);
        assertEquals((byte) (word >>> 24), bytes[0]);
        assertEquals((byte) (word >>> 16), bytes[1]);
        assertEquals((byte) (word >>> 8), bytes[2]);
        assertEquals((byte) word, bytes[3]);
    }

    @Test
    void rejectsFieldsThatDoNotFitInAByte() {
        assertThrows(IllegalArgumentException.class,
                () -> new MapleFrameHeader(MapleCommand.GET_CONDITION, 0x100, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new MapleFrameHeader(MapleCommand.GET_CONDITION, 0, 0x100, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new MapleFrameHeader(MapleCommand.GET_CONDITION, 0, 0, 0x100));
    }
}
