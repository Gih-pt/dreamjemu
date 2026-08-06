package org.dreamjemu.gpu.pvr2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaParameterControlWordTest {

    @Test
    void encodeDecodeRoundTrips() {
        TaParameterControlWord original = new TaParameterControlWord(
                TaParameterType.VERTEX_PARAMETER, true, TileAcceleratorListType.TRANSLUCENT_MODIFIERS, false,
                TaStripLength.SIX_STRIPS, TaUserTileClipMode.OUTSIDE_ENABLE, true, true, TaColorType.FLOATING_COLOR,
                true, false, true, false);

        TaParameterControlWord decoded = TaParameterControlWord.decode(original.encode());

        assertEquals(original, decoded);
    }

    /**
     * Literal regression test: "Global Parameters for object 0 (0x8086 002E)"
     * from §3.7.6.3 "Parameter Input Example" in Sega's "Dreamcast/Dev.Box
     * System Architecture" manual, documented alongside as "2strip, Clip
     * inside enable, Intensity, Textured, use Offset, Gouraud, 32bitUV".
     */
    @Test
    void decodesTheOfficialManualsFirstWorkedExample() {
        TaParameterControlWord decoded = TaParameterControlWord.decode(0x8086002E);

        assertEquals(TaParameterType.POLYGON_OR_MODIFIER_VOLUME, decoded.paraType());
        assertTrue(decoded.groupEnable());
        assertEquals(TaStripLength.TWO_STRIPS, decoded.stripLength());
        assertEquals(TaUserTileClipMode.INSIDE_ENABLE, decoded.userTileClip());
        assertEquals(TaColorType.INTENSITY_MODE_1, decoded.colorType());
        assertTrue(decoded.texture());
        assertTrue(decoded.offset());
        assertTrue(decoded.gouraud());
        assertFalse(decoded.sixteenBitUv()); // "32bitUV" means NOT 16-bit
        assertFalse(decoded.shadow());
        assertFalse(decoded.volume());

        // And the encoder must produce the exact same literal word back.
        assertEquals(0x8086002E, decoded.encode());
    }

    /**
     * Literal regression test: "Global Parameters for object 2 (0x8088 004A)"
     * from the same worked example, documented as "4strip, Clip disable, Two
     * Volume, Packed, Textured, no Offset, Gouraud, 32bitUV".
     */
    @Test
    void decodesTheOfficialManualsSecondWorkedExample() {
        TaParameterControlWord decoded = TaParameterControlWord.decode(0x8088004A);

        assertEquals(TaParameterType.POLYGON_OR_MODIFIER_VOLUME, decoded.paraType());
        assertTrue(decoded.groupEnable());
        assertEquals(TaStripLength.FOUR_STRIPS, decoded.stripLength());
        assertEquals(TaUserTileClipMode.DISABLED, decoded.userTileClip());
        assertTrue(decoded.volume()); // "Two Volume"
        assertEquals(TaColorType.PACKED_COLOR, decoded.colorType());
        assertTrue(decoded.texture());
        assertFalse(decoded.offset()); // "no Offset"
        assertTrue(decoded.gouraud());
        assertFalse(decoded.sixteenBitUv()); // "32bitUV"

        assertEquals(0x8088004A, decoded.encode());
    }

    @Test
    void paraTypeOccupiesTheTopThreeBits() {
        TaParameterControlWord control = new TaParameterControlWord(
                TaParameterType.VERTEX_PARAMETER, false, TileAcceleratorListType.OPAQUE_POLYGONS, false,
                TaStripLength.ONE_STRIP, TaUserTileClipMode.DISABLED, false, false, TaColorType.PACKED_COLOR,
                false, false, false, false);

        assertEquals(0b111 << 29, control.encode());
    }

    @Test
    void endOfStripIsBit28() {
        TaParameterControlWord control = new TaParameterControlWord(
                TaParameterType.END_OF_LIST, true, TileAcceleratorListType.OPAQUE_POLYGONS, false,
                TaStripLength.ONE_STRIP, TaUserTileClipMode.DISABLED, false, false, TaColorType.PACKED_COLOR,
                false, false, false, false);

        assertEquals(1 << 28, control.encode());
    }

    @Test
    void listTypeOccupiesBits26To24() {
        TaParameterControlWord control = new TaParameterControlWord(
                TaParameterType.END_OF_LIST, false, TileAcceleratorListType.PUNCH_THROUGH_POLYGONS, false,
                TaStripLength.ONE_STRIP, TaUserTileClipMode.DISABLED, false, false, TaColorType.PACKED_COLOR,
                false, false, false, false);

        assertEquals(0b100 << 24, control.encode());
    }
}
