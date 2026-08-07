package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PvrObjectBufferAddressTest {

    @Test
    void encodeDecodeRoundTrips() {
        PvrObjectBufferAddress original = new PvrObjectBufferAddress(0x300000);

        PvrObjectBufferAddress decoded = PvrObjectBufferAddress.decode(original.encode());

        assertEquals(original, decoded);
    }

    @Test
    void zeroIsAValidAlignedAddress() {
        PvrObjectBufferAddress address = new PvrObjectBufferAddress(0);

        assertEquals(0, address.encode());
    }

    @Test
    void rejectsAddressNotOneMebibyteAligned() {
        assertThrows(IllegalArgumentException.class, () -> new PvrObjectBufferAddress(0x100001));
        assertThrows(IllegalArgumentException.class, () -> new PvrObjectBufferAddress(1));
    }

    @Test
    void rejectsAddressBeyondTwentyFourBits() {
        assertThrows(IllegalArgumentException.class, () -> new PvrObjectBufferAddress(0x1000000));
    }

    @Test
    void decodeSurfacesAMisalignedRawValueRatherThanSilentlyMaskingIt() {
        // A real register read that violates its own documented "bits 0-19
        // are always zero" invariant is worth knowing about, not hiding.
        assertThrows(IllegalArgumentException.class, () -> PvrObjectBufferAddress.decode(0x300001));
    }
}
