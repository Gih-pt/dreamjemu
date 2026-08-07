package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PvrClipRangeTest {

    @Test
    void encodeDecodeRoundTrips() {
        PvrClipRange original = new PvrClipRange(0x010, 0x27F);

        PvrClipRange decoded = PvrClipRange.decode(original.encode());

        assertEquals(original, decoded);
    }

    @Test
    void minOccupiesTheLowElevenBits() {
        PvrClipRange range = new PvrClipRange(0x7FF, 0);

        assertEquals(0x7FF, range.encode());
    }

    @Test
    void maxOccupiesBits26To16() {
        PvrClipRange range = new PvrClipRange(0, 0x7FF);

        assertEquals(0x7FF << 16, range.encode());
    }

    @Test
    void bothAddressConstantsAreDistinct() {
        assertEquals(0xA05F8068, PvrClipRange.HORIZONTAL_REGISTER_ADDRESS);
        assertEquals(0xA05F806C, PvrClipRange.VERTICAL_REGISTER_ADDRESS);
    }

    @Test
    void rejectsOutOfRangeFields() {
        assertThrows(IllegalArgumentException.class, () -> new PvrClipRange(0x800, 0));
        assertThrows(IllegalArgumentException.class, () -> new PvrClipRange(0, 0x800));
        assertThrows(IllegalArgumentException.class, () -> new PvrClipRange(-1, 0));
    }
}
