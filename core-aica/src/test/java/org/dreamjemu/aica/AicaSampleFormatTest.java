package org.dreamjemu.aica;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AicaSampleFormatTest {

    @Test
    void fieldValuesMatchTheDocumentedEncoding() {
        assertEquals(0, AicaSampleFormat.PCM_16BIT.fieldValue());
        assertEquals(1, AicaSampleFormat.PCM_8BIT.fieldValue());
        assertEquals(2, AicaSampleFormat.ADPCM_4BIT.fieldValue());
        assertEquals(3, AicaSampleFormat.ADPCM_4BIT_LOOPING.fieldValue());
    }

    @Test
    void fromFieldValueRoundTripsForEveryConstant() {
        for (AicaSampleFormat format : AicaSampleFormat.values()) {
            assertEquals(format, AicaSampleFormat.fromFieldValue(format.fieldValue()));
        }
    }

    @Test
    void fromFieldValueRejectsOutOfRangeInput() {
        assertThrows(IllegalArgumentException.class, () -> AicaSampleFormat.fromFieldValue(4));
        assertThrows(IllegalArgumentException.class, () -> AicaSampleFormat.fromFieldValue(-1));
    }
}
