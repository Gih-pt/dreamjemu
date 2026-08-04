package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PvrTileBufferSizeTest {

    @Test
    void encodesUsingTheHardwaresMinusOneConvention() {
        // A 20x15 tile buffer (a common 640x480 framebuffer: 640/32=20, 480/32=15)
        // must be stored as (20-1, 15-1) = (19, 14) in the raw register.
        PvrTileBufferSize size = new PvrTileBufferSize(15, 20);

        int encoded = size.encode();

        assertEquals(14, (encoded >>> 16) & 0xFFFF); // height field
        assertEquals(19, encoded & 0xFFFF);           // width field
    }

    @Test
    void encodeDecodeRoundTrips() {
        PvrTileBufferSize original = new PvrTileBufferSize(15, 20);

        PvrTileBufferSize decoded = PvrTileBufferSize.decode(original.encode());

        assertEquals(original, decoded);
    }

    @Test
    void pixelDimensionsAreTilesTimes32() {
        PvrTileBufferSize size = new PvrTileBufferSize(15, 20);

        assertEquals(480, size.heightInPixels());
        assertEquals(640, size.widthInPixels());
    }

    @Test
    void minimumOneTileEncodesAsZeroField() {
        PvrTileBufferSize size = new PvrTileBufferSize(1, 1);

        assertEquals(0, size.encode());
        assertEquals(size, PvrTileBufferSize.decode(0));
    }

    @Test
    void maximumTileCountRoundTrips() {
        // The largest representable count: a 16-bit "minus 1" field maxes at 65535 -> 65536 tiles.
        PvrTileBufferSize size = new PvrTileBufferSize(65536, 65536);

        PvrTileBufferSize decoded = PvrTileBufferSize.decode(size.encode());

        assertEquals(size, decoded);
    }

    @Test
    void rejectsZeroOrNegativeTileCounts() {
        assertThrows(IllegalArgumentException.class, () -> new PvrTileBufferSize(0, 10));
        assertThrows(IllegalArgumentException.class, () -> new PvrTileBufferSize(10, 0));
        assertThrows(IllegalArgumentException.class, () -> new PvrTileBufferSize(-1, 10));
    }

    @Test
    void rejectsTileCountsBeyondTheSixteenBitField() {
        assertThrows(IllegalArgumentException.class, () -> new PvrTileBufferSize(65537, 10));
    }
}
