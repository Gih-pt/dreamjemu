package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PvrDisplayPixelFormatTest {

    @Test
    void fieldValuesMatchTheDocumentedEncoding() {
        assertEquals(0, PvrDisplayPixelFormat.RGB0555.fieldValue());
        assertEquals(1, PvrDisplayPixelFormat.RGB565.fieldValue());
        assertEquals(2, PvrDisplayPixelFormat.RGB888.fieldValue());
        assertEquals(3, PvrDisplayPixelFormat.RGB0888.fieldValue());
    }

    @Test
    void fromFieldValueRoundTripsForEveryConstant() {
        for (PvrDisplayPixelFormat format : PvrDisplayPixelFormat.values()) {
            assertEquals(format, PvrDisplayPixelFormat.fromFieldValue(format.fieldValue()));
        }
    }

    @Test
    void fromFieldValueRejectsOutOfRangeInput() {
        assertThrows(IllegalArgumentException.class, () -> PvrDisplayPixelFormat.fromFieldValue(4));
        assertThrows(IllegalArgumentException.class, () -> PvrDisplayPixelFormat.fromFieldValue(-1));
    }
}
