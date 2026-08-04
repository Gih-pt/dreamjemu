package org.dreamjemu.maple;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControllerConditionTest {

    @Test
    void encodedLengthIs8Bytes() {
        assertEquals(8, ControllerCondition.neutral().encode().length);
        assertEquals(8, ControllerCondition.ENCODED_LENGTH);
    }

    @Test
    void neutralConditionHasNoButtonsPressed() {
        ControllerCondition neutral = ControllerCondition.neutral();
        for (ControllerButton button : ControllerButton.values()) {
            assertFalse(neutral.isPressed(button));
        }
    }

    @Test
    void withButtonPressedClearsOnlyThatBit() {
        ControllerCondition condition = ControllerCondition.neutral().withButton(ControllerButton.A, true);

        assertTrue(condition.isPressed(ControllerButton.A));
        assertFalse(condition.isPressed(ControllerButton.B));
        assertFalse(condition.isPressed(ControllerButton.START));
        // Every other bit is still 1 (released).
        assertEquals(0xFFFF & ~ControllerButton.A.mask(), condition.buttons());
    }

    @Test
    void withButtonReleasedSetsBitBack() {
        ControllerCondition pressed = ControllerCondition.neutral().withButton(ControllerButton.START, true);
        ControllerCondition released = pressed.withButton(ControllerButton.START, false);

        assertFalse(released.isPressed(ControllerButton.START));
        assertEquals(0xFFFF, released.buttons());
    }

    @Test
    void bitPositionsMatchTheDocumentedLayout() {
        // Spot-check against mc.pp.se/dc/controller.html's bit table.
        assertEquals(0, ControllerButton.C.bit());
        assertEquals(2, ControllerButton.A.bit());
        assertEquals(3, ControllerButton.START.bit());
        assertEquals(4, ControllerButton.UP.bit());
        assertEquals(7, ControllerButton.RIGHT.bit());
        assertEquals(11, ControllerButton.D.bit());
        assertEquals(15, ControllerButton.RIGHT2.bit());
    }

    @Test
    void encodeDecodeRoundTrips() {
        ControllerCondition original = new ControllerCondition(0xFFFF & ~ControllerButton.X.mask(), 0x10, 0x20, 0x7F,
                0x81, 0x00, 0xFF);

        ControllerCondition decoded = ControllerCondition.decode(original.encode(), 0);

        assertEquals(original, decoded);
    }

    @Test
    void buttonsFieldIsLittleEndianOnTheWire() {
        // 0x1234 -> low byte (0x34) first.
        ControllerCondition condition = new ControllerCondition(0x1234, 0, 0, 0, 0, 0, 0);

        byte[] encoded = condition.encode();

        assertEquals(0x34, encoded[0] & 0xFF);
        assertEquals(0x12, encoded[1] & 0xFF);
    }

    @Test
    void triggerAndJoystickByteOrderMatchesStructLayout() {
        ControllerCondition condition = new ControllerCondition(0, 0xAA, 0xBB, 0xCC, 0xDD, 0xEE, 0xFF);

        byte[] encoded = condition.encode();

        assertEquals(0xAA, encoded[2] & 0xFF); // rtrigger
        assertEquals(0xBB, encoded[3] & 0xFF); // ltrigger
        assertEquals(0xCC, encoded[4] & 0xFF); // joyx
        assertEquals(0xDD, encoded[5] & 0xFF); // joyy
        assertEquals(0xEE, encoded[6] & 0xFF); // joyx2
        assertEquals(0xFF, encoded[7] & 0xFF); // joyy2
    }

    @Test
    void rejectsOutOfRangeFields() {
        assertThrows(IllegalArgumentException.class,
                () -> new ControllerCondition(0x10000, 0, 0, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ControllerCondition(0, 256, 0, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ControllerCondition(0, 0, -1, 0, 0, 0, 0));
    }

    @Test
    void decodeThrowsWhenNotEnoughDataRemains() {
        byte[] tooShort = new byte[4];
        assertThrows(IllegalArgumentException.class, () -> ControllerCondition.decode(tooShort, 0));
    }
}
