package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TileAcceleratorListTypeTest {

    @Test
    void fieldValuesMatchTheDocumentedHolly2ListTypeCodes() {
        assertEquals(0, TileAcceleratorListType.OPAQUE_POLYGONS.fieldValue());
        assertEquals(1, TileAcceleratorListType.OPAQUE_MODIFIERS.fieldValue());
        assertEquals(2, TileAcceleratorListType.TRANSLUCENT_POLYGONS.fieldValue());
        assertEquals(3, TileAcceleratorListType.TRANSLUCENT_MODIFIERS.fieldValue());
        assertEquals(4, TileAcceleratorListType.PUNCH_THROUGH_POLYGONS.fieldValue());
    }

    @Test
    void fromFieldValueRoundTripsForEveryConstant() {
        for (TileAcceleratorListType listType : TileAcceleratorListType.values()) {
            assertEquals(listType, TileAcceleratorListType.fromFieldValue(listType.fieldValue()));
        }
    }

    @Test
    void fromFieldValueRejectsOutOfRangeInput() {
        assertThrows(IllegalArgumentException.class, () -> TileAcceleratorListType.fromFieldValue(5));
        assertThrows(IllegalArgumentException.class, () -> TileAcceleratorListType.fromFieldValue(-1));
    }
}
