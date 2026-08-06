package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaParameterTypeTest {

    @Test
    void fieldValuesMatchTheDocumentedEncoding() {
        assertEquals(0, TaParameterType.END_OF_LIST.fieldValue());
        assertEquals(1, TaParameterType.USER_TILE_CLIP.fieldValue());
        assertEquals(2, TaParameterType.OBJECT_LIST_SET.fieldValue());
        assertEquals(3, TaParameterType.CONTROL_RESERVED.fieldValue());
        assertEquals(4, TaParameterType.POLYGON_OR_MODIFIER_VOLUME.fieldValue());
        assertEquals(5, TaParameterType.SPRITE.fieldValue());
        assertEquals(6, TaParameterType.GLOBAL_RESERVED.fieldValue());
        assertEquals(7, TaParameterType.VERTEX_PARAMETER.fieldValue());
    }

    @Test
    void fromFieldValueRoundTripsForEveryConstant() {
        for (TaParameterType type : TaParameterType.values()) {
            assertEquals(type, TaParameterType.fromFieldValue(type.fieldValue()));
        }
    }

    @Test
    void fromFieldValueRejectsOutOfRangeInput() {
        assertThrows(IllegalArgumentException.class, () -> TaParameterType.fromFieldValue(8));
        assertThrows(IllegalArgumentException.class, () -> TaParameterType.fromFieldValue(-1));
    }
}
