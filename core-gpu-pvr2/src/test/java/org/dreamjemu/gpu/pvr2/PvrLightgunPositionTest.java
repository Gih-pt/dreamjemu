package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PvrLightgunPositionTest {

    @Test
    void decodesVerticalAndHorizontalPositionsIndependently() {
        int value = (0x123 << 16) | 0x0AB;

        PvrLightgunPosition position = PvrLightgunPosition.decode(value);

        assertEquals(0x123, position.verticalPosition());
        assertEquals(0x0AB, position.horizontalPosition());
    }

    @Test
    void decodeIgnoresTheUnmodeledTopBitsAndTheGapBetweenTheFields() {
        int garbageTopBits = 0b111111 << 26;
        int garbageGap = 0b111111 << 10;
        int value = garbageTopBits | (0x200 << 16) | garbageGap | 0x100;

        PvrLightgunPosition position = PvrLightgunPosition.decode(value);

        assertEquals(0x200, position.verticalPosition());
        assertEquals(0x100, position.horizontalPosition());
    }

    @Test
    void maximumTenBitPositionsDecodeCorrectly() {
        int value = (0x3FF << 16) | 0x3FF;

        PvrLightgunPosition position = PvrLightgunPosition.decode(value);

        assertEquals(0x3FF, position.verticalPosition());
        assertEquals(0x3FF, position.horizontalPosition());
    }
}
