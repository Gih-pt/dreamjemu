package org.dreamjemu.maple;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapleDmaEnableTest {

    @Test
    void encodeDecodeRoundTrips() {
        assertEquals(new MapleDmaEnable(true), MapleDmaEnable.decode(new MapleDmaEnable(true).encode()));
        assertEquals(new MapleDmaEnable(false), MapleDmaEnable.decode(new MapleDmaEnable(false).encode()));
    }

    @Test
    void matchesTheDocumentedRawValues() {
        assertEquals(1, new MapleDmaEnable(true).encode());
        assertEquals(0, new MapleDmaEnable(false).encode());
    }

    @Test
    void decodeIgnoresUpperBits() {
        MapleDmaEnable decoded = MapleDmaEnable.decode(0xFFFFFFFE | 1);

        assertTrue(decoded.enabled());
    }

    @Test
    void decodeOfZeroIsDisabled() {
        assertFalse(MapleDmaEnable.decode(0).enabled());
    }
}
