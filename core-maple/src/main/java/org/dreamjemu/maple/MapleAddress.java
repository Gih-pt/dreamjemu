package org.dreamjemu.maple;

/**
 * Encodes/decodes the 8-bit Maple bus address format used for both the
 * sender and recipient fields of a {@link MapleFrameHeader}.
 *
 * <p>Layout (source: Marcus Comstedt, "Dreamcast Programming - Maple Bus",
 * mc.pp.se/dc/maplebus.html, "Address format" section):
 *
 * <pre>
 * bit  7-6 : port number (0 = port A .. 3 = port D)
 * bit  5   : main peripheral present/addressed
 * bit  4   : sub-peripheral 5
 * bit  3   : sub-peripheral 4
 * bit  2   : sub-peripheral 3
 * bit  1   : sub-peripheral 2
 * bit  0   : sub-peripheral 1
 * </pre>
 *
 * <p>The address of the Dreamcast host port itself is obtained when none
 * of bits 0-5 are set (only the port number bits are present). When a
 * main peripheral identifies itself in a response, it sets the
 * sub-peripheral bit for each sub-peripheral connected to it, in addition
 * to bit 5.
 */
public final class MapleAddress {

    private static final int MAIN_PERIPHERAL_BIT = 0x20;
    private static final int SUB_PERIPHERAL_MASK = 0x1F;
    private static final int PORT_SHIFT = 6;

    private MapleAddress() {
    }

    /**
     * @param port                 port number, 0 (A) to 3 (D)
     * @param mainPeripheral       whether this address refers to (or is sent
     *                             by) the main peripheral on the port
     * @param subPeripheralMask    a 5-bit mask, bit 0 = sub-peripheral 1
     *                             through bit 4 = sub-peripheral 5
     */
    public static int encode(int port, boolean mainPeripheral, int subPeripheralMask) {
        if (port < 0 || port > 3) {
            throw new IllegalArgumentException("Maple port must be 0-3, got " + port);
        }
        if ((subPeripheralMask & ~SUB_PERIPHERAL_MASK) != 0) {
            throw new IllegalArgumentException(
                    "Sub-peripheral mask must fit in 5 bits, got 0x" + Integer.toHexString(subPeripheralMask));
        }
        int value = port << PORT_SHIFT;
        if (mainPeripheral) {
            value |= MAIN_PERIPHERAL_BIT;
        }
        value |= subPeripheralMask;
        return value & 0xFF;
    }

    /** The address of the Dreamcast host itself on a given port. */
    public static int host(int port) {
        return encode(port, false, 0);
    }

    public static int port(int address) {
        return (address >> PORT_SHIFT) & 0x3;
    }

    public static boolean isMainPeripheral(int address) {
        return (address & MAIN_PERIPHERAL_BIT) != 0;
    }

    public static int subPeripheralMask(int address) {
        return address & SUB_PERIPHERAL_MASK;
    }

    public static boolean isHost(int address) {
        return (address & (MAIN_PERIPHERAL_BIT | SUB_PERIPHERAL_MASK)) == 0;
    }
}
