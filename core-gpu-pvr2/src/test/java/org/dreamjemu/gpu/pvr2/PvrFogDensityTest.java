package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PvrFogDensityTest {

    @Test
    void encodeDecodeRoundTrips() {
        PvrFogDensity original = new PvrFogDensity(0x7F, 0x10);

        PvrFogDensity decoded = PvrFogDensity.decode(original.encode());

        assertEquals(original, decoded);
    }

    @Test
    void mantissaOccupiesBits15To8() {
        PvrFogDensity density = new PvrFogDensity(0xFF, 0);

        assertEquals(0xFF00, density.encode());
    }

    @Test
    void exponentOccupiesBits7To0() {
        PvrFogDensity density = new PvrFogDensity(0, 0xFF);

        assertEquals(0xFF, density.encode());
    }

    @Test
    void decodeIgnoresUpperSixteenBits() {
        PvrFogDensity decoded = PvrFogDensity.decode(0xFFFF7F10);

        assertEquals(new PvrFogDensity(0x7F, 0x10), decoded);
    }

    @Test
    void rejectsOutOfRangeFields() {
        assertThrows(IllegalArgumentException.class, () -> new PvrFogDensity(256, 0));
        assertThrows(IllegalArgumentException.class, () -> new PvrFogDensity(0, -1));
    }
}
