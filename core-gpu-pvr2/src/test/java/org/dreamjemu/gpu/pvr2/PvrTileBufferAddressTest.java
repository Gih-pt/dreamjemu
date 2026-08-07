package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PvrTileBufferAddressTest {

    @Test
    void encodeDecodeRoundTrips() {
        PvrTileBufferAddress original = new PvrTileBufferAddress(0x200008);

        PvrTileBufferAddress decoded = PvrTileBufferAddress.decode(original.encode());

        assertEquals(original, decoded);
    }

    @Test
    void zeroIsAValidAlignedAddress() {
        assertEquals(0, new PvrTileBufferAddress(0).encode());
    }

    @Test
    void rejectsAddressNotEightByteAligned() {
        assertThrows(IllegalArgumentException.class, () -> new PvrTileBufferAddress(1));
        assertThrows(IllegalArgumentException.class, () -> new PvrTileBufferAddress(0x100004));
    }

    @Test
    void rejectsAddressBeyondTwentyFourBits() {
        assertThrows(IllegalArgumentException.class, () -> new PvrTileBufferAddress(0x1000000));
    }

    @Test
    void decodeSurfacesAMisalignedRawValueRatherThanSilentlyMaskingIt() {
        assertThrows(IllegalArgumentException.class, () -> PvrTileBufferAddress.decode(0x200001));
    }
}
