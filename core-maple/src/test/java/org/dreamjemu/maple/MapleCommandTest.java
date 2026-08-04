package org.dreamjemu.maple;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MapleCommandTest {

    @Test
    void everyDocumentedCodeResolves() {
        assertEquals(MapleCommand.REQUEST_DEVICE_INFO, MapleCommand.fromCode(1));
        assertEquals(MapleCommand.REQUEST_EXTENDED_DEVICE_INFO, MapleCommand.fromCode(2));
        assertEquals(MapleCommand.RESET_DEVICE, MapleCommand.fromCode(3));
        assertEquals(MapleCommand.SHUTDOWN_DEVICE, MapleCommand.fromCode(4));
        assertEquals(MapleCommand.DEVICE_INFO, MapleCommand.fromCode(5));
        assertEquals(MapleCommand.EXTENDED_DEVICE_INFO, MapleCommand.fromCode(6));
        assertEquals(MapleCommand.COMMAND_ACK, MapleCommand.fromCode(7));
        assertEquals(MapleCommand.DATA_TRANSFER, MapleCommand.fromCode(8));
        assertEquals(MapleCommand.GET_CONDITION, MapleCommand.fromCode(9));
        assertEquals(MapleCommand.GET_MEMORY_INFO, MapleCommand.fromCode(10));
        assertEquals(MapleCommand.BLOCK_READ, MapleCommand.fromCode(11));
        assertEquals(MapleCommand.BLOCK_WRITE, MapleCommand.fromCode(12));
        assertEquals(MapleCommand.SET_CONDITION, MapleCommand.fromCode(14));
        assertEquals(MapleCommand.NO_RESPONSE, MapleCommand.fromCode(-1));
        assertEquals(MapleCommand.FUNCTION_CODE_UNSUPPORTED, MapleCommand.fromCode(-2));
        assertEquals(MapleCommand.UNKNOWN_COMMAND, MapleCommand.fromCode(-3));
        assertEquals(MapleCommand.RESEND_REQUEST, MapleCommand.fromCode(-4));
        assertEquals(MapleCommand.FILE_ERROR, MapleCommand.fromCode(-5));
    }

    @Test
    void codeRoundTripsForEveryEnumConstant() {
        for (MapleCommand command : MapleCommand.values()) {
            assertEquals(command, MapleCommand.fromCode(command.code()));
        }
    }

    @Test
    void unknownCodeThrowsInsteadOfReturningAPlaceholder() {
        // 13 and 0 are gaps in the documented command space.
        assertThrows(IllegalArgumentException.class, () -> MapleCommand.fromCode(13));
        assertThrows(IllegalArgumentException.class, () -> MapleCommand.fromCode(0));
    }
}
