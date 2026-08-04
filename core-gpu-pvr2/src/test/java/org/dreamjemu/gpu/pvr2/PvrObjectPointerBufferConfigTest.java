package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.dreamjemu.gpu.pvr2.ObjectPointerBufferSize.DISABLED;
import static org.dreamjemu.gpu.pvr2.ObjectPointerBufferSize.SIZE_16;
import static org.dreamjemu.gpu.pvr2.ObjectPointerBufferSize.SIZE_32;
import static org.dreamjemu.gpu.pvr2.ObjectPointerBufferSize.SIZE_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PvrObjectPointerBufferConfigTest {

    @Test
    void encodeDecodeRoundTrips() {
        PvrObjectPointerBufferConfig original = new PvrObjectPointerBufferConfig(
                true, SIZE_32, SIZE_16, SIZE_8, DISABLED, SIZE_16);

        PvrObjectPointerBufferConfig decoded = PvrObjectPointerBufferConfig.decode(original.encode());

        assertEquals(original, decoded);
    }

    @Test
    void eachListTypeOccupiesItsOwnNonOverlappingField() {
        // Setting only one field to a nonzero size must not disturb any other field's bits.
        PvrObjectPointerBufferConfig onlyOpaquePolygons = new PvrObjectPointerBufferConfig(
                false, SIZE_32, DISABLED, DISABLED, DISABLED, DISABLED);
        assertEquals(0b11, onlyOpaquePolygons.encode());

        PvrObjectPointerBufferConfig onlyPunchThrough = new PvrObjectPointerBufferConfig(
                false, DISABLED, DISABLED, DISABLED, DISABLED, SIZE_32);
        assertEquals(0b11 << 16, onlyPunchThrough.encode());
    }

    @Test
    void growsDownwardBitIsBit20() {
        PvrObjectPointerBufferConfig config = new PvrObjectPointerBufferConfig(
                true, DISABLED, DISABLED, DISABLED, DISABLED, DISABLED);

        assertEquals(1 << 20, config.encode());
    }

    @Test
    void sizeForLooksUpTheMatchingField() {
        PvrObjectPointerBufferConfig config = new PvrObjectPointerBufferConfig(
                false, SIZE_8, SIZE_16, SIZE_32, DISABLED, SIZE_8);

        assertEquals(SIZE_8, config.sizeFor(TileAcceleratorListType.OPAQUE_POLYGONS));
        assertEquals(SIZE_16, config.sizeFor(TileAcceleratorListType.OPAQUE_MODIFIERS));
        assertEquals(SIZE_32, config.sizeFor(TileAcceleratorListType.TRANSLUCENT_POLYGONS));
        assertEquals(DISABLED, config.sizeFor(TileAcceleratorListType.TRANSLUCENT_MODIFIERS));
        assertEquals(SIZE_8, config.sizeFor(TileAcceleratorListType.PUNCH_THROUGH_POLYGONS));
    }

    @Test
    void decodeExtractsGrowthDirectionCorrectly() {
        assertFalse(PvrObjectPointerBufferConfig.decode(0).growsDownward());
        assertTrue(PvrObjectPointerBufferConfig.decode(1 << 20).growsDownward());
    }
}
