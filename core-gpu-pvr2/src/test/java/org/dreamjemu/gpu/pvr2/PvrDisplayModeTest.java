package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PvrDisplayModeTest {

    private static PvrDisplayMode sample() {
        return new PvrDisplayMode(true, false, 0x7F, 0b101, PvrDisplayPixelFormat.RGB565, true, true);
    }

    @Test
    void encodeDecodeRoundTrips() {
        PvrDisplayMode original = sample();

        PvrDisplayMode decoded = PvrDisplayMode.decode(original.encode());

        assertEquals(original, decoded);
    }

    @Test
    void clockBitIsBit23() {
        PvrDisplayMode mode = new PvrDisplayMode(true, false, 0, 0, PvrDisplayPixelFormat.RGB0555, false, false);

        assertEquals(1 << 23, mode.encode());
    }

    @Test
    void stripenBitIsBit22() {
        PvrDisplayMode mode = new PvrDisplayMode(false, true, 0, 0, PvrDisplayPixelFormat.RGB0555, false, false);

        assertEquals(1 << 22, mode.encode());
    }

    @Test
    void thresholdOccupiesBits15To8() {
        PvrDisplayMode mode = new PvrDisplayMode(false, false, 0xFF, 0, PvrDisplayPixelFormat.RGB0555, false, false);

        assertEquals(0xFF << 8, mode.encode());
    }

    @Test
    void extendOccupiesBits6To4() {
        PvrDisplayMode mode = new PvrDisplayMode(false, false, 0, 0b111, PvrDisplayPixelFormat.RGB0555, false, false);

        assertEquals(0b111 << 4, mode.encode());
    }

    @Test
    void pixelModeOccupiesBits3And2() {
        PvrDisplayMode mode = new PvrDisplayMode(false, false, 0, 0, PvrDisplayPixelFormat.RGB0888, false, false);

        assertEquals(0b11 << 2, mode.encode());
    }

    @Test
    void lineDoubleAndEnableAreBits1And0() {
        PvrDisplayMode lineDoubleOnly = new PvrDisplayMode(false, false, 0, 0, PvrDisplayPixelFormat.RGB0555, true, false);
        assertEquals(0b10, lineDoubleOnly.encode());

        PvrDisplayMode enableOnly = new PvrDisplayMode(false, false, 0, 0, PvrDisplayPixelFormat.RGB0555, false, true);
        assertEquals(0b01, enableOnly.encode());
    }

    @Test
    void decodeIgnoresUnmodeledStriplenBits() {
        // striplen (bits 21-16) is deliberately not modeled - garbage there
        // must not affect the fields that are.
        int garbageStriplen = 0b111111 << 16;
        PvrDisplayMode decoded = PvrDisplayMode.decode(garbageStriplen | sample().encode());

        assertEquals(sample(), decoded);
    }

    @Test
    void rejectsOutOfRangeThresholdOrExtend() {
        assertThrows(IllegalArgumentException.class,
                () -> new PvrDisplayMode(false, false, 256, 0, PvrDisplayPixelFormat.RGB0555, false, false));
        assertThrows(IllegalArgumentException.class,
                () -> new PvrDisplayMode(false, false, 0, 8, PvrDisplayPixelFormat.RGB0555, false, false));
    }
}
