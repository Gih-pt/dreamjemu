package org.dreamjemu.maple;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapleAddressTest {

    @Test
    void hostAddressHasOnlyPortBitsSet() {
        for (int port = 0; port <= 3; port++) {
            int address = MapleAddress.host(port);
            assertTrue(MapleAddress.isHost(address));
            assertEquals(port, MapleAddress.port(address));
            assertFalse(MapleAddress.isMainPeripheral(address));
            assertEquals(0, MapleAddress.subPeripheralMask(address));
        }
    }

    @Test
    void encodesMainPeripheralOnPortB() {
        int address = MapleAddress.encode(1, true, 0);
        assertEquals(0x60, address);
        assertEquals(1, MapleAddress.port(address));
        assertTrue(MapleAddress.isMainPeripheral(address));
        assertFalse(MapleAddress.isHost(address));
    }

    @Test
    void encodesSubPeripheralsAlongsideMainPeripheral() {
        // Sub-peripheral 1 (bit 0) and sub-peripheral 5 (bit 4) connected, port D.
        int subMask = 0b10001;
        int address = MapleAddress.encode(3, true, subMask);
        assertEquals(3, MapleAddress.port(address));
        assertTrue(MapleAddress.isMainPeripheral(address));
        assertEquals(subMask, MapleAddress.subPeripheralMask(address));
    }

    @Test
    void rejectsOutOfRangePort() {
        assertThrows(IllegalArgumentException.class, () -> MapleAddress.encode(4, false, 0));
        assertThrows(IllegalArgumentException.class, () -> MapleAddress.encode(-1, false, 0));
    }

    @Test
    void rejectsSubPeripheralMaskWiderThanFiveBits() {
        assertThrows(IllegalArgumentException.class, () -> MapleAddress.encode(0, true, 0x20));
    }
}
