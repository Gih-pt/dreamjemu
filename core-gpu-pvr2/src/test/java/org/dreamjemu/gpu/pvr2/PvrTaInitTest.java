package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PvrTaInitTest {

    @Test
    void triggerWordSetsOnlyTheTopBit() {
        assertEquals(0x80000000, PvrTaInit.triggerWord());
    }

    @Test
    void encodeDecodeRoundTrips() {
        assertEquals(new PvrTaInit(true), PvrTaInit.decode(new PvrTaInit(true).encode()));
        assertEquals(new PvrTaInit(false), PvrTaInit.decode(new PvrTaInit(false).encode()));
    }

    @Test
    void decodeIgnoresLowerBits() {
        PvrTaInit decoded = PvrTaInit.decode(0x80000001);

        assertTrue(decoded.initialize());
    }

    @Test
    void nonTopBitAloneDoesNotSetInitialize() {
        PvrTaInit decoded = PvrTaInit.decode(0x7FFFFFFF);

        assertFalse(decoded.initialize());
    }
}
