package org.dreamjemu.maple;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MapleDmaCommandTableAddressTest {

    @Test
    void encodeDecodeRoundTrips() {
        MapleDmaCommandTableAddress original = new MapleDmaCommandTableAddress(0x0C700000);

        MapleDmaCommandTableAddress decoded = MapleDmaCommandTableAddress.decode(original.encode());

        assertEquals(original, decoded);
    }

    @Test
    void decodesTheOfficialWorkedExamplesAddress() {
        // §2.6.8's worked example sets SB_MDSTAR to 0x0C700000.
        MapleDmaCommandTableAddress decoded = MapleDmaCommandTableAddress.decode(0x0C700000);

        assertEquals(0x0C700000, decoded.address());
    }

    @Test
    void rejectsAddressNotThirtyTwoByteAligned() {
        assertThrows(IllegalArgumentException.class, () -> new MapleDmaCommandTableAddress(0x0C700001));
        assertThrows(IllegalArgumentException.class, () -> new MapleDmaCommandTableAddress(0x0C700010));
    }

    @Test
    void rejectsAddressOutsideTheFixedTopThreeBitsWindow() {
        // Bits 31-29 must be 000 per the source ("fixed") - 0x20000000 sets bit 29.
        assertThrows(IllegalArgumentException.class, () -> new MapleDmaCommandTableAddress(0x20000000));
    }

    @Test
    void lowestAndHighestDocumentedAddressesAreValid() {
        // Source's documented settable range: 0x0C000000 to 0x0FFFFFE0.
        assertEquals(0x0C000000, new MapleDmaCommandTableAddress(0x0C000000).encode());
        assertEquals(0x0FFFFFE0, new MapleDmaCommandTableAddress(0x0FFFFFE0).encode());
    }
}
