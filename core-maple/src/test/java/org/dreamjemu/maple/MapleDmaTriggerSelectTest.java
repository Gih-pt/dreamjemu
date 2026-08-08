package org.dreamjemu.maple;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MapleDmaTriggerSelectTest {

    @Test
    void fieldValuesMatchTheDocumentedEncoding() {
        assertEquals(0, MapleDmaTrigger.SOFTWARE.fieldValue());
        assertEquals(1, MapleDmaTrigger.V_BLANK.fieldValue());
    }

    @Test
    void fromFieldValueRoundTripsForEveryConstant() {
        for (MapleDmaTrigger trigger : MapleDmaTrigger.values()) {
            assertEquals(trigger, MapleDmaTrigger.fromFieldValue(trigger.fieldValue()));
        }
    }

    @Test
    void fromFieldValueRejectsOutOfRangeInput() {
        assertThrows(IllegalArgumentException.class, () -> MapleDmaTrigger.fromFieldValue(2));
        assertThrows(IllegalArgumentException.class, () -> MapleDmaTrigger.fromFieldValue(-1));
    }

    @Test
    void registerEncodeDecodeRoundTrips() {
        MapleDmaTriggerSelect original = new MapleDmaTriggerSelect(MapleDmaTrigger.V_BLANK);

        MapleDmaTriggerSelect decoded = MapleDmaTriggerSelect.decode(original.encode());

        assertEquals(original, decoded);
    }

    @Test
    void decodeIgnoresUpperBits() {
        MapleDmaTriggerSelect decoded = MapleDmaTriggerSelect.decode(0xFFFFFFFE | 1);

        assertEquals(MapleDmaTrigger.V_BLANK, decoded.trigger());
    }
}
