package org.dreamjemu.maple;

/**
 * Bit positions of the digital buttons bitfield in the Controller
 * function's ({@link MapleFunctionCode#CONTROLLER}) condition structure.
 *
 * <p>Source: Marcus Comstedt, "Dreamcast Programming - Controllers"
 * (mc.pp.se/dc/controller.html). <strong>Note the bitfield's own
 * polarity is inverted from the usual convention</strong>: a 0 bit means
 * the button is pressed, and 1 means released — see
 * {@link ControllerCondition#isPressed(ControllerButton)}, which accounts
 * for this rather than leaving it as an easy-to-miss trap for callers.
 */
public enum ControllerButton {
    C(0),
    B(1),
    A(2),
    START(3),
    UP(4),
    DOWN(5),
    LEFT(6),
    RIGHT(7),
    Z(8),
    Y(9),
    X(10),
    D(11),
    UP2(12),
    DOWN2(13),
    LEFT2(14),
    RIGHT2(15);

    private final int bit;

    ControllerButton(int bit) {
        this.bit = bit;
    }

    public int bit() {
        return bit;
    }

    public int mask() {
        return 1 << bit;
    }
}
