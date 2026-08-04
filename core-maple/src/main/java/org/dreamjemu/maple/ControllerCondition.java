package org.dreamjemu.maple;

/**
 * The condition structure for the Controller function
 * ({@link MapleFunctionCode#CONTROLLER}), as returned by a
 * {@code GET_CONDITION} request (and sent by {@code SET_CONDITION}).
 *
 * <p>Source: Marcus Comstedt, "Dreamcast Programming - Controllers"
 * (mc.pp.se/dc/controller.html), "Gamepad Condition structure" section:
 *
 * <pre>
 * int16 buttons   ; digital buttons bitfield (little endian)
 * int8  rtrigger  ; right analogue trigger (0-255)
 * int8  ltrigger  ; left analogue trigger (0-255)
 * int8  joyx      ; analogue joystick X (0-255)
 * int8  joyy      ; analogue joystick Y (0-255)
 * int8  joyx2     ; second analogue joystick X (0-255)
 * int8  joyy2     ; second analogue joystick Y (0-255)
 * </pre>
 *
 * <p>8 bytes total (2 words). The buttons bitfield is inverted from the
 * usual convention: a 0 bit means the corresponding button is pressed,
 * and 1 means released — see {@link #isPressed}.
 */
public record ControllerCondition(int buttons, int rtrigger, int ltrigger, int joyx, int joyy, int joyx2,
                                   int joyy2) {

    public static final int ENCODED_LENGTH = 8;

    public ControllerCondition {
        requireUnsigned16("buttons", buttons);
        requireUnsigned8("rtrigger", rtrigger);
        requireUnsigned8("ltrigger", ltrigger);
        requireUnsigned8("joyx", joyx);
        requireUnsigned8("joyy", joyy);
        requireUnsigned8("joyx2", joyx2);
        requireUnsigned8("joyy2", joyy2);
    }

    private static void requireUnsigned8(String name, int value) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(name + " must be 0-255, got " + value);
        }
    }

    private static void requireUnsigned16(String name, int value) {
        if (value < 0 || value > 0xFFFF) {
            throw new IllegalArgumentException(name + " must be 0-65535, got " + value);
        }
    }

    /**
     * A neutral condition: every button released, both triggers at rest
     * (0), and both joysticks centered (0x80, the documented resting
     * value for an unsigned 0-255 axis).
     */
    public static ControllerCondition neutral() {
        return new ControllerCondition(0xFFFF, 0, 0, 0x80, 0x80, 0x80, 0x80);
    }

    /** True if {@code button}'s bit is 0 (pressed), per the inverted bitfield convention. */
    public boolean isPressed(ControllerButton button) {
        return (buttons & button.mask()) == 0;
    }

    /** Returns a copy with {@code button} marked pressed (bit cleared) or released (bit set). */
    public ControllerCondition withButton(ControllerButton button, boolean pressed) {
        int newButtons = pressed ? (buttons & ~button.mask()) : (buttons | button.mask());
        return new ControllerCondition(newButtons, rtrigger, ltrigger, joyx, joyy, joyx2, joyy2);
    }

    public byte[] encode() {
        byte[] out = new byte[ENCODED_LENGTH];
        out[0] = (byte) buttons;
        out[1] = (byte) (buttons >>> 8);
        out[2] = (byte) rtrigger;
        out[3] = (byte) ltrigger;
        out[4] = (byte) joyx;
        out[5] = (byte) joyy;
        out[6] = (byte) joyx2;
        out[7] = (byte) joyy2;
        return out;
    }

    public static ControllerCondition decode(byte[] data, int offset) {
        if (offset < 0 || offset + ENCODED_LENGTH > data.length) {
            throw new IllegalArgumentException("Not enough data to decode a ControllerCondition at offset " + offset);
        }
        int buttons = (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
        int rtrigger = data[offset + 2] & 0xFF;
        int ltrigger = data[offset + 3] & 0xFF;
        int joyx = data[offset + 4] & 0xFF;
        int joyy = data[offset + 5] & 0xFF;
        int joyx2 = data[offset + 6] & 0xFF;
        int joyy2 = data[offset + 7] & 0xFF;
        return new ControllerCondition(buttons, rtrigger, ltrigger, joyx, joyy, joyx2, joyy2);
    }
}
