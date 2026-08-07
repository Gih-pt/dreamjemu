package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PvrShadowConfigTest {

    @Test
    void encodeDecodeRoundTrips() {
        PvrShadowConfig original = new PvrShadowConfig(true, 0x80);

        PvrShadowConfig decoded = PvrShadowConfig.decode(original.encode());

        assertEquals(original, decoded);
    }

    @Test
    void enableIsBit8() {
        PvrShadowConfig config = new PvrShadowConfig(true, 0);

        assertEquals(1 << 8, config.encode());
    }

    @Test
    void intensityOccupiesTheLowestByte() {
        PvrShadowConfig config = new PvrShadowConfig(false, 0xFF);

        assertEquals(0xFF, config.encode());
    }

    @Test
    void rejectsOutOfRangeIntensity() {
        assertThrows(IllegalArgumentException.class, () -> new PvrShadowConfig(false, 256));
        assertThrows(IllegalArgumentException.class, () -> new PvrShadowConfig(false, -1));
    }
}
