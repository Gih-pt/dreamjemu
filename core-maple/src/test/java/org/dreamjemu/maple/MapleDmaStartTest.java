package org.dreamjemu.maple;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapleDmaStartTest {

    @Test
    void encodeDecodeRoundTrips() {
        assertEquals(new MapleDmaStart(true), MapleDmaStart.decode(new MapleDmaStart(true).encode()));
        assertEquals(new MapleDmaStart(false), MapleDmaStart.decode(new MapleDmaStart(false).encode()));
    }

    @Test
    void startTriggerWordMatchesTheOfficialWorkedExample() {
        // §2.6.8: "Write 0x00000001 in the SB_MDST register to initiate Maple-DMA."
        assertEquals(0x00000001, MapleDmaStart.startTriggerWord());
    }

    @Test
    void decodeIgnoresUpperBits() {
        MapleDmaStart decoded = MapleDmaStart.decode(0xFFFFFFFE | 1);

        assertTrue(decoded.inProgress());
    }
}
