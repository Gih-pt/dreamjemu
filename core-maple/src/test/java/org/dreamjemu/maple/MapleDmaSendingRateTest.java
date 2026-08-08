package org.dreamjemu.maple;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MapleDmaSendingRateTest {

    @Test
    void fieldValuesMatchTheDocumentedEncoding() {
        assertEquals(0, MapleDmaSendingRate.TWO_MBPS.fieldValue());
        assertEquals(1, MapleDmaSendingRate.ONE_MBPS.fieldValue());
    }

    @Test
    void fromFieldValueRoundTripsForEveryConstant() {
        for (MapleDmaSendingRate rate : MapleDmaSendingRate.values()) {
            assertEquals(rate, MapleDmaSendingRate.fromFieldValue(rate.fieldValue()));
        }
    }

    @Test
    void fromFieldValueRejectsTheUndocumentedValues() {
        assertThrows(IllegalArgumentException.class, () -> MapleDmaSendingRate.fromFieldValue(2));
        assertThrows(IllegalArgumentException.class, () -> MapleDmaSendingRate.fromFieldValue(3));
    }
}
