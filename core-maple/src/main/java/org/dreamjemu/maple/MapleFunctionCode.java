package org.dreamjemu.maple;

/**
 * Maple bus function codes: the bit-field values a peripheral advertises
 * (OR'd together) to declare which kinds of function it implements, and
 * that commands like {@code GET_CONDITION}/{@code SET_CONDITION} use to
 * select which function's data they're addressing.
 *
 * <p>Source: Marcus Comstedt, "Dreamcast Programming - Maple Bus"
 * (mc.pp.se/dc/maplebus.html), "Function codes" section. Verified against
 * that page before implementation, same discipline as every other
 * hardware-format constant in this project (see docs/STATUS.md).
 */
public final class MapleFunctionCode {

    public static final int CONTROLLER = 0x001;
    public static final int MEMORY_CARD = 0x002;
    public static final int LCD_DISPLAY = 0x004;
    public static final int CLOCK = 0x008;
    public static final int MICROPHONE = 0x010;
    public static final int AR_GUN = 0x020;
    public static final int KEYBOARD = 0x040;
    public static final int LIGHT_GUN = 0x080;
    public static final int PURU_PURU_PACK = 0x100;
    public static final int MOUSE = 0x200;

    private MapleFunctionCode() {
    }
}
