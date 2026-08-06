package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaStripLengthTest {

    @Test
    void fieldValuesMatchTheDocumentedEncoding() {
        assertEquals(0, TaStripLength.ONE_STRIP.fieldValue());
        assertEquals(1, TaStripLength.TWO_STRIPS.fieldValue());
        assertEquals(2, TaStripLength.FOUR_STRIPS.fieldValue());
        assertEquals(3, TaStripLength.SIX_STRIPS.fieldValue());
    }

    @Test
    void stripCountsMatchTheDocumentedCounts() {
        assertEquals(1, TaStripLength.ONE_STRIP.stripCount());
        assertEquals(2, TaStripLength.TWO_STRIPS.stripCount());
        assertEquals(4, TaStripLength.FOUR_STRIPS.stripCount());
        assertEquals(6, TaStripLength.SIX_STRIPS.stripCount());
    }

    @Test
    void fromFieldValueRoundTripsForEveryConstant() {
        for (TaStripLength length : TaStripLength.values()) {
            assertEquals(length, TaStripLength.fromFieldValue(length.fieldValue()));
        }
    }

    @Test
    void fromFieldValueRejectsOutOfRangeInput() {
        assertThrows(IllegalArgumentException.class, () -> TaStripLength.fromFieldValue(4));
        assertThrows(IllegalArgumentException.class, () -> TaStripLength.fromFieldValue(-1));
    }
}
