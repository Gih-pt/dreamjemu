package org.dreamjemu.maple;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MapleSystemControlTest {

    @Test
    void encodeDecodeRoundTrips() {
        MapleSystemControl original = new MapleSystemControl(0x3A98, true, MapleDmaSendingRate.ONE_MBPS, 5);

        MapleSystemControl decoded = MapleSystemControl.decode(original.encode());

        assertEquals(original, decoded);
    }

    /**
     * Literal regression test: §2.6.8's worked example sets SB_MSYS to
     * 0xC3500000, documented in prose as "timeout 1ms, transfer rate 2Mbps".
     */
    @Test
    void decodesTheOfficialWorkedExample() {
        MapleSystemControl decoded = MapleSystemControl.decode(0xC3500000);

        // 0xC350 counts * 20ns = 1,000,000ns = 1ms.
        assertEquals(0xC350, decoded.timeoutCounter());
        assertEquals(1_000_000L, decoded.timeoutNanoseconds());
        assertEquals(MapleDmaSendingRate.TWO_MBPS, decoded.sendingRate());
    }

    @Test
    void defaultConstantMatchesTheDocumentedDefaults() {
        assertEquals(0x3A98, MapleSystemControl.DEFAULT.timeoutCounter());
        assertFalse(MapleSystemControl.DEFAULT.singleHardTriggerManual());
        assertEquals(MapleDmaSendingRate.TWO_MBPS, MapleSystemControl.DEFAULT.sendingRate());
        assertEquals(0, MapleSystemControl.DEFAULT.delayTime());
    }

    @Test
    void delayTimeMicrosecondsConversion() {
        MapleSystemControl control = new MapleSystemControl(0, false, MapleDmaSendingRate.TWO_MBPS, 3);

        assertEquals(3900L, control.delayTimeMicroseconds());
    }

    @Test
    void rejectsDelayTimeAtOrAboveTheProhibitedThreshold() {
        assertThrows(IllegalArgumentException.class,
                () -> new MapleSystemControl(0, false, MapleDmaSendingRate.TWO_MBPS, 11));
    }

    @Test
    void allowsTheMaximumDocumentedDelayTime() {
        MapleSystemControl control = new MapleSystemControl(0, false, MapleDmaSendingRate.TWO_MBPS, 10);

        assertEquals(10, control.delayTime());
    }

    @Test
    void singleHardTriggerBitIsBit12() {
        MapleSystemControl control = new MapleSystemControl(0, true, MapleDmaSendingRate.TWO_MBPS, 0);

        assertEquals(1 << 12, control.encode());
    }
}
