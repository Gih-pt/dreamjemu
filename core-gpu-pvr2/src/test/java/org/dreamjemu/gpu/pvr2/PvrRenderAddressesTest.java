package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PvrRenderAddressesTest {

    @Test
    void encodeDecodeRoundTrips() {
        PvrRenderAddresses original = new PvrRenderAddresses(true, 0x001000, false, 0x002000);

        PvrRenderAddresses decoded = PvrRenderAddresses.decode(
                original.encodeOddFieldRegister(), original.encodeEvenFieldRegister());

        assertEquals(original, decoded);
    }

    @Test
    void txBitIsBit24OfEachRegisterIndependently() {
        PvrRenderAddresses oddOnly = new PvrRenderAddresses(true, 0, false, 0);
        assertEquals(1 << 24, oddOnly.encodeOddFieldRegister());
        assertEquals(0, oddOnly.encodeEvenFieldRegister());

        PvrRenderAddresses evenOnly = new PvrRenderAddresses(false, 0, true, 0);
        assertEquals(0, evenOnly.encodeOddFieldRegister());
        assertEquals(1 << 24, evenOnly.encodeEvenFieldRegister());
    }

    @Test
    void addressesOccupyTheLowTwentyFourBits() {
        PvrRenderAddresses addresses = new PvrRenderAddresses(false, 0xABCDEF, false, 0x123456);

        assertEquals(0xABCDEF, addresses.encodeOddFieldRegister());
        assertEquals(0x123456, addresses.encodeEvenFieldRegister());
    }

    @Test
    void rejectsAddressBeyondTwentyFourBits() {
        assertThrows(IllegalArgumentException.class, () -> new PvrRenderAddresses(false, 0x1000000, false, 0));
        assertThrows(IllegalArgumentException.class, () -> new PvrRenderAddresses(false, 0, false, 0x1000000));
    }
}
