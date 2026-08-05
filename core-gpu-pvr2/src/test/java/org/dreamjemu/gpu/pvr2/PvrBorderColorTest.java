package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PvrBorderColorTest {

    @Test
    void encodeDecodeRoundTrips() {
        PvrBorderColor original = new PvrBorderColor(0x11, 0x22, 0x33);

        PvrBorderColor decoded = PvrBorderColor.decode(original.encode());

        assertEquals(original, decoded);
    }

    @Test
    void channelsOccupyStandardRgb888ByteOrder() {
        PvrBorderColor color = new PvrBorderColor(0xAA, 0xBB, 0xCC);

        assertEquals(0xAABBCC, color.encode());
    }

    @Test
    void blackAndWhiteEncodeAsExpected() {
        assertEquals(0x000000, new PvrBorderColor(0, 0, 0).encode());
        assertEquals(0xFFFFFF, new PvrBorderColor(255, 255, 255).encode());
    }

    @Test
    void rejectsChannelsOutOfByteRange() {
        assertThrows(IllegalArgumentException.class, () -> new PvrBorderColor(256, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new PvrBorderColor(0, -1, 0));
    }

    @Test
    void decodeIgnoresUpperByte() {
        PvrBorderColor decoded = PvrBorderColor.decode(0xFF112233);

        assertEquals(new PvrBorderColor(0x11, 0x22, 0x33), decoded);
    }
}
