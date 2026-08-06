package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaUserTileClipModeTest {

    @Test
    void fieldValuesMatchTheDocumentedEncoding() {
        assertEquals(0, TaUserTileClipMode.DISABLED.fieldValue());
        assertEquals(1, TaUserTileClipMode.RESERVED.fieldValue());
        assertEquals(2, TaUserTileClipMode.INSIDE_ENABLE.fieldValue());
        assertEquals(3, TaUserTileClipMode.OUTSIDE_ENABLE.fieldValue());
    }

    @Test
    void fromFieldValueRoundTripsForEveryConstant() {
        for (TaUserTileClipMode mode : TaUserTileClipMode.values()) {
            assertEquals(mode, TaUserTileClipMode.fromFieldValue(mode.fieldValue()));
        }
    }

    @Test
    void fromFieldValueRejectsOutOfRangeInput() {
        assertThrows(IllegalArgumentException.class, () -> TaUserTileClipMode.fromFieldValue(4));
        assertThrows(IllegalArgumentException.class, () -> TaUserTileClipMode.fromFieldValue(-1));
    }
}
