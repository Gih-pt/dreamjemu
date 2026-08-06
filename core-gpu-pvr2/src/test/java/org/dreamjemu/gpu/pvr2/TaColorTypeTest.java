package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaColorTypeTest {

    @Test
    void fieldValuesMatchTheDocumentedEncoding() {
        assertEquals(0, TaColorType.PACKED_COLOR.fieldValue());
        assertEquals(1, TaColorType.FLOATING_COLOR.fieldValue());
        assertEquals(2, TaColorType.INTENSITY_MODE_1.fieldValue());
        assertEquals(3, TaColorType.INTENSITY_MODE_2.fieldValue());
    }

    @Test
    void fromFieldValueRoundTripsForEveryConstant() {
        for (TaColorType type : TaColorType.values()) {
            assertEquals(type, TaColorType.fromFieldValue(type.fieldValue()));
        }
    }

    @Test
    void fromFieldValueRejectsOutOfRangeInput() {
        assertThrows(IllegalArgumentException.class, () -> TaColorType.fromFieldValue(4));
        assertThrows(IllegalArgumentException.class, () -> TaColorType.fromFieldValue(-1));
    }
}
