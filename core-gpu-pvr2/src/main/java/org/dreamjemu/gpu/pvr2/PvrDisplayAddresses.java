package org.dreamjemu.gpu.pvr2;

/**
 * The two VRAM addresses the display reads from — separate addresses for
 * odd and even fields, which matters for interlaced output. Modeled
 * together since they're always used as a pair, the same reasoning
 * {@code core-aica}'s {@code AicaLoopRange} uses for its own register
 * pair.
 *
 * <p>Source: "Guide to the PowerVR-chip of the Dreamcast" by Lars Olsson,
 * v0.51 (hitmen.c02.at/files/docs/dc/powervr-reg.txt):
 *
 * <pre>
 * a05f8050 (fb_display_addr1)
 * +--------------+
 * | 31-24 | 23-0 |
 * |  n/a  | addr |
 * +--------------+
 * addr: Address in VRAM for displaying odd fields (32-bit aligned)
 *
 * a05f8054 (fb_display_addr2)
 * +--------------+
 * | 31-24 | 23-0 |
 * |  n/a  | addr |
 * +--------------+
 * addr: Address in VRAM for displaying even fields (32-bit aligned)
 * </pre>
 *
 * <p>The source documents these addresses as 32-bit aligned but doesn't
 * state what happens on a misaligned value, so alignment isn't enforced
 * here — only that each address fits in the documented 24-bit field.
 */
public record PvrDisplayAddresses(int oddFieldAddress, int evenFieldAddress) {

    /** P2 (uncached) address of the odd-field register. */
    public static final int ODD_FIELD_REGISTER_ADDRESS = 0xA05F8050;

    /** P2 (uncached) address of the even-field register. */
    public static final int EVEN_FIELD_REGISTER_ADDRESS = 0xA05F8054;

    private static final int ADDRESS_MASK = 0xFFFFFF;

    public PvrDisplayAddresses {
        requireFitsInField("oddFieldAddress", oddFieldAddress);
        requireFitsInField("evenFieldAddress", evenFieldAddress);
    }

    private static void requireFitsInField(String name, int address) {
        if ((address & ~ADDRESS_MASK) != 0) {
            throw new IllegalArgumentException(name + " must fit in 24 bits, got 0x" + Integer.toHexString(address));
        }
    }

    public int encodeOddFieldAddress() {
        return oddFieldAddress & ADDRESS_MASK;
    }

    public int encodeEvenFieldAddress() {
        return evenFieldAddress & ADDRESS_MASK;
    }

    public static PvrDisplayAddresses decode(int oddFieldValue, int evenFieldValue) {
        return new PvrDisplayAddresses(oddFieldValue & ADDRESS_MASK, evenFieldValue & ADDRESS_MASK);
    }
}
