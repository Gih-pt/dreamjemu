package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PvrDisplayAddressesTest {

    @Test
    void encodeDecodeRoundTrips() {
        PvrDisplayAddresses original = new PvrDisplayAddresses(0x001000, 0x002000);

        PvrDisplayAddresses decoded = PvrDisplayAddresses.decode(
                original.encodeOddFieldAddress(), original.encodeEvenFieldAddress());

        assertEquals(original, decoded);
    }

    @Test
    void oddAndEvenAddressesAreIndependent() {
        PvrDisplayAddresses addresses = new PvrDisplayAddresses(0x000100, 0x000200);

        assertEquals(0x000100, addresses.encodeOddFieldAddress());
        assertEquals(0x000200, addresses.encodeEvenFieldAddress());
    }

    @Test
    void maximumTwentyFourBitAddressRoundTrips() {
        PvrDisplayAddresses addresses = new PvrDisplayAddresses(0xFFFFFF, 0xFFFFFF);

        assertEquals(addresses, PvrDisplayAddresses.decode(addresses.encodeOddFieldAddress(), addresses.encodeEvenFieldAddress()));
    }

    @Test
    void rejectsAddressBeyondTwentyFourBits() {
        assertThrows(IllegalArgumentException.class, () -> new PvrDisplayAddresses(0x1000000, 0));
        assertThrows(IllegalArgumentException.class, () -> new PvrDisplayAddresses(0, 0x1000000));
    }

    @Test
    void decodeMasksOffTheUpperByte() {
        PvrDisplayAddresses decoded = PvrDisplayAddresses.decode(0xAB001000, 0xCD002000);

        assertEquals(new PvrDisplayAddresses(0x001000, 0x002000), decoded);
    }
}
