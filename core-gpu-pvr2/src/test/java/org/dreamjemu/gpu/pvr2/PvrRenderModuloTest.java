package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PvrRenderModuloTest {

    @Test
    void encodeDecodeRoundTrips() {
        PvrRenderModulo original = new PvrRenderModulo(0x1AB);

        PvrRenderModulo decoded = PvrRenderModulo.decode(original.encode());

        assertEquals(original, decoded);
    }

    @Test
    void maximumNineBitValueRoundTrips() {
        PvrRenderModulo modulo = new PvrRenderModulo(0x1FF);

        assertEquals(0x1FF, modulo.encode());
    }

    @Test
    void decodeIgnoresUpperBits() {
        PvrRenderModulo decoded = PvrRenderModulo.decode(0xFFFFFE00 | 0x50);

        assertEquals(new PvrRenderModulo(0x50), decoded);
    }

    @Test
    void rejectsOutOfRangeValue() {
        assertThrows(IllegalArgumentException.class, () -> new PvrRenderModulo(0x200));
        assertThrows(IllegalArgumentException.class, () -> new PvrRenderModulo(-1));
    }
}
