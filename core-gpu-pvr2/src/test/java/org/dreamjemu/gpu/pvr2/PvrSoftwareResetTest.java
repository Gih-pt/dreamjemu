package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PvrSoftwareResetTest {

    @Test
    void normalHasEveryLineInactive() {
        PvrSoftwareReset normal = PvrSoftwareReset.normal();

        assertFalse(normal.resetVramBus());
        assertFalse(normal.resetPvrCore());
        assertFalse(normal.resetTileAccelerator());
        assertEquals(0, normal.encode());
    }

    @Test
    void eachResetLineOccupiesItsOwnBit() {
        assertEquals(0x4, new PvrSoftwareReset(true, false, false).encode());
        assertEquals(0x2, new PvrSoftwareReset(false, true, false).encode());
        assertEquals(0x1, new PvrSoftwareReset(false, false, true).encode());
        assertEquals(0x7, new PvrSoftwareReset(true, true, true).encode());
    }

    @Test
    void encodeDecodeRoundTrips() {
        PvrSoftwareReset original = new PvrSoftwareReset(true, false, true);

        PvrSoftwareReset decoded = PvrSoftwareReset.decode(original.encode());

        assertEquals(original, decoded);
    }

    @Test
    void decodeIgnoresUnrelatedBits() {
        // Bits above bit 2 are documented n/a - decode must not be thrown off by garbage there.
        PvrSoftwareReset decoded = PvrSoftwareReset.decode(0xFFFFFFF8 | 0x2);

        assertFalse(decoded.resetVramBus());
        assertTrue(decoded.resetPvrCore());
        assertFalse(decoded.resetTileAccelerator());
    }
}
