package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PvrPaletteConfigTest {

    @Test
    void fieldValuesMatchTheDocumentedEncoding() {
        assertEquals(0, PvrPaletteMode.ARGB1555.fieldValue());
        assertEquals(1, PvrPaletteMode.RGB565.fieldValue());
        assertEquals(2, PvrPaletteMode.ARGB4444.fieldValue());
        assertEquals(3, PvrPaletteMode.ARGB8888.fieldValue());
    }

    @Test
    void fromFieldValueRoundTripsForEveryConstant() {
        for (PvrPaletteMode mode : PvrPaletteMode.values()) {
            assertEquals(mode, PvrPaletteMode.fromFieldValue(mode.fieldValue()));
        }
    }

    @Test
    void fromFieldValueRejectsOutOfRangeInput() {
        assertThrows(IllegalArgumentException.class, () -> PvrPaletteMode.fromFieldValue(4));
        assertThrows(IllegalArgumentException.class, () -> PvrPaletteMode.fromFieldValue(-1));
    }

    @Test
    void configEncodeDecodeRoundTrips() {
        for (PvrPaletteMode mode : PvrPaletteMode.values()) {
            PvrPaletteConfig config = new PvrPaletteConfig(mode);
            assertEquals(config, PvrPaletteConfig.decode(config.encode()));
        }
    }

    @Test
    void configDecodeIgnoresUpperBits() {
        PvrPaletteConfig decoded = PvrPaletteConfig.decode(0xFFFFFFFC | 0b10);

        assertEquals(new PvrPaletteConfig(PvrPaletteMode.ARGB4444), decoded);
    }
}
