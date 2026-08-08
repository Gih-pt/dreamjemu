package org.dreamjemu.maple;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapleStatusTest {

    @Test
    void decodesFieldsIndependently() {
        // moveStatus=1, frameMonitor=0b101, stateMonitor=0b010101, lineMonitor=0xAA
        int value = (1 << 31) | (0b101 << 24) | (0b010101 << 16) | 0xAA;

        MapleStatus status = MapleStatus.decode(value);

        assertTrue(status.operating());
        assertEquals(0b101, status.internalFrameMonitor());
        assertEquals(0b010101, status.internalStateMonitor());
        assertEquals(0xAA, status.lineMonitor());
    }

    @Test
    void defaultResetValueDecodesAsNotOperatingWithAllLinesHigh() {
        // Documented defaults: Move Status=0, Line Monitor=0xFF.
        MapleStatus status = MapleStatus.decode(0x000000FF);

        assertFalse(status.operating());
        assertEquals(0xFF, status.lineMonitor());
    }

    @Test
    void lineMonitorAccessorsMatchTheDocumentedBitPositions() {
        MapleStatus status = MapleStatus.decode(0b10101010); // bits 7,5,3,1 set

        assertTrue(status.portDSdckaLine());   // bit 7
        assertFalse(status.portDSdckbLine());  // bit 6
        assertTrue(status.portCSdckaLine());   // bit 5
        assertFalse(status.portCSdckbLine());  // bit 4
        assertTrue(status.portBSdckaLine());   // bit 3
        assertFalse(status.portBSdckbLine());  // bit 2
        assertTrue(status.portASdckaLine());   // bit 1
        assertFalse(status.portASdckbLine());  // bit 0
    }
}
