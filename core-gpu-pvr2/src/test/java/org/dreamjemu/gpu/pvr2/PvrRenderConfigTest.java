package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PvrRenderConfigTest {

    @Test
    void encodeDecodeRoundTrips() {
        PvrRenderConfig original = new PvrRenderConfig(0x80, 0xFF, true, PvrRenderPixelFormat.ARGB8888);

        PvrRenderConfig decoded = PvrRenderConfig.decode(original.encode());

        assertEquals(original, decoded);
    }

    @Test
    void thresholdOccupiesBits23To16() {
        PvrRenderConfig config = new PvrRenderConfig(0xFF, 0, false, PvrRenderPixelFormat.RGB0555);

        assertEquals(0xFF << 16, config.encode());
    }

    @Test
    void alphaOccupiesBits15To8() {
        PvrRenderConfig config = new PvrRenderConfig(0, 0xFF, false, PvrRenderPixelFormat.RGB0555);

        assertEquals(0xFF << 8, config.encode());
    }

    @Test
    void ditherIsBit3() {
        PvrRenderConfig config = new PvrRenderConfig(0, 0, true, PvrRenderPixelFormat.RGB0555);

        assertEquals(1 << 3, config.encode());
    }

    @Test
    void pixelFormatOccupiesTheLowestThreeBits() {
        PvrRenderConfig config = new PvrRenderConfig(0, 0, false, PvrRenderPixelFormat.ARGB4444_ALTERNATE);

        assertEquals(7, config.encode());
    }

    @Test
    void rejectsOutOfRangeThresholdOrAlpha() {
        assertThrows(IllegalArgumentException.class,
                () -> new PvrRenderConfig(256, 0, false, PvrRenderPixelFormat.RGB0555));
        assertThrows(IllegalArgumentException.class,
                () -> new PvrRenderConfig(0, -1, false, PvrRenderPixelFormat.RGB0555));
    }
}
