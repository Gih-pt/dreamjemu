package org.dreamjemu.gpu.pvr2;

/**
 * The two VRAM addresses the renderer writes to — separate addresses
 * for odd and even fields, each with its own write-width flag. Modeled
 * together as a pair, same reasoning as {@link PvrDisplayAddresses} and
 * {@code core-aica}'s {@code AicaLoopRange}.
 *
 * <p>Source: "Guide to the PowerVR-chip of the Dreamcast" by Lars Olsson,
 * v0.51 (hitmen.c02.at/files/docs/dc/powervr-reg.txt):
 *
 * <pre>
 * a05f8060 (fb_render_addr1)
 * +-------------------+
 * | 31-25 | 24 | 23-0 |
 * | n/a   | tx | addr |
 * +-------------------+
 * tx: 0 = 32-bit write, 1 = 64-bit write (for rendering to textures)
 * addr: Address in VRAM for rendering of odd fields.
 *
 * a05f8064 (fb_render_addr2)
 * +-------------------+
 * | 31-25 | 24 | 23-0 |
 * | n/a   | tx | addr |
 * +-------------------+
 * tx: 0 = 32-bit write, 1 = 64-bit write (for rendering to textures)
 * addr: Address in VRAM for rendering of even fields.
 * </pre>
 */
public record PvrRenderAddresses(boolean oddFieldSixtyFourBitWrite, int oddFieldAddress,
                                  boolean evenFieldSixtyFourBitWrite, int evenFieldAddress) {

    /** P2 (uncached) address of the odd-field register. */
    public static final int ODD_FIELD_REGISTER_ADDRESS = 0xA05F8060;

    /** P2 (uncached) address of the even-field register. */
    public static final int EVEN_FIELD_REGISTER_ADDRESS = 0xA05F8064;

    private static final int TX_BIT = 24;
    private static final int ADDRESS_MASK = 0xFFFFFF;

    public PvrRenderAddresses {
        requireFitsInField("oddFieldAddress", oddFieldAddress);
        requireFitsInField("evenFieldAddress", evenFieldAddress);
    }

    private static void requireFitsInField(String name, int address) {
        if ((address & ~ADDRESS_MASK) != 0) {
            throw new IllegalArgumentException(name + " must fit in 24 bits, got 0x" + Integer.toHexString(address));
        }
    }

    public int encodeOddFieldRegister() {
        int value = oddFieldAddress & ADDRESS_MASK;
        if (oddFieldSixtyFourBitWrite) {
            value |= 1 << TX_BIT;
        }
        return value;
    }

    public int encodeEvenFieldRegister() {
        int value = evenFieldAddress & ADDRESS_MASK;
        if (evenFieldSixtyFourBitWrite) {
            value |= 1 << TX_BIT;
        }
        return value;
    }

    public static PvrRenderAddresses decode(int oddFieldValue, int evenFieldValue) {
        boolean oddTx = ((oddFieldValue >>> TX_BIT) & 1) != 0;
        int oddAddr = oddFieldValue & ADDRESS_MASK;
        boolean evenTx = ((evenFieldValue >>> TX_BIT) & 1) != 0;
        int evenAddr = evenFieldValue & ADDRESS_MASK;
        return new PvrRenderAddresses(oddTx, oddAddr, evenTx, evenAddr);
    }
}
