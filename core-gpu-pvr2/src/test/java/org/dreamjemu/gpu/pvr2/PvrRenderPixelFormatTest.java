package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PvrRenderPixelFormatTest {

    @Test
    void fieldValuesMatchTheDocumentedEncoding() {
        assertEquals(0, PvrRenderPixelFormat.RGB0555.fieldValue());
        assertEquals(1, PvrRenderPixelFormat.RGB565.fieldValue());
        assertEquals(2, PvrRenderPixelFormat.ARGB4444.fieldValue());
        assertEquals(3, PvrRenderPixelFormat.ARGB1555.fieldValue());
        assertEquals(4, PvrRenderPixelFormat.RGB888.fieldValue());
        assertEquals(5, PvrRenderPixelFormat.RGB0888.fieldValue());
        assertEquals(6, PvrRenderPixelFormat.ARGB8888.fieldValue());
        assertEquals(7, PvrRenderPixelFormat.ARGB4444_ALTERNATE.fieldValue());
    }

    @Test
    void fromFieldValueRoundTripsForEveryConstant() {
        for (PvrRenderPixelFormat format : PvrRenderPixelFormat.values()) {
            assertEquals(format, PvrRenderPixelFormat.fromFieldValue(format.fieldValue()));
        }
    }

    @Test
    void theDisputedDuplicateValueStaysDistinctFromArgb4444() {
        // The source flags value 7 as possibly the same as value 2 ("same as 2?")
        // but doesn't confirm it - they must decode to different constants so
        // round-tripping value 7 doesn't silently become value 2.
        assertNotEquals(PvrRenderPixelFormat.ARGB4444, PvrRenderPixelFormat.ARGB4444_ALTERNATE);
        assertEquals(PvrRenderPixelFormat.ARGB4444_ALTERNATE, PvrRenderPixelFormat.fromFieldValue(7));
    }

    @Test
    void fromFieldValueRejectsOutOfRangeInput() {
        assertThrows(IllegalArgumentException.class, () -> PvrRenderPixelFormat.fromFieldValue(8));
        assertThrows(IllegalArgumentException.class, () -> PvrRenderPixelFormat.fromFieldValue(-1));
    }
}
