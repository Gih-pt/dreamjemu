package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObjectPointerBufferSizeTest {

    @Test
    void fieldValuesMatchTheDocumentedEncoding() {
        assertEquals(0, ObjectPointerBufferSize.DISABLED.fieldValue());
        assertEquals(1, ObjectPointerBufferSize.SIZE_8.fieldValue());
        assertEquals(2, ObjectPointerBufferSize.SIZE_16.fieldValue());
        assertEquals(3, ObjectPointerBufferSize.SIZE_32.fieldValue());
    }

    @Test
    void objectPointerCountsMatchTheDocumentedCapacities() {
        assertEquals(0, ObjectPointerBufferSize.DISABLED.objectPointerCount());
        assertEquals(7, ObjectPointerBufferSize.SIZE_8.objectPointerCount());
        assertEquals(15, ObjectPointerBufferSize.SIZE_16.objectPointerCount());
        assertEquals(31, ObjectPointerBufferSize.SIZE_32.objectPointerCount());
    }

    @Test
    void fromFieldValueRoundTripsForEveryConstant() {
        for (ObjectPointerBufferSize size : ObjectPointerBufferSize.values()) {
            assertEquals(size, ObjectPointerBufferSize.fromFieldValue(size.fieldValue()));
        }
    }

    @Test
    void fromFieldValueRejectsOutOfRangeInput() {
        assertThrows(IllegalArgumentException.class, () -> ObjectPointerBufferSize.fromFieldValue(4));
        assertThrows(IllegalArgumentException.class, () -> ObjectPointerBufferSize.fromFieldValue(-1));
    }
}
