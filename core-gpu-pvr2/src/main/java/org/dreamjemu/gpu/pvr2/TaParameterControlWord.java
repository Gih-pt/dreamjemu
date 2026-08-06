package org.dreamjemu.gpu.pvr2;

/**
 * The Parameter Control Word: the first 4 bytes of every Control,
 * Global, and Vertex Parameter fed to the Tile Accelerator. Determines
 * the data configuration and type of the parameter it heads.
 *
 * <p>Source: Sega's "Dreamcast/Dev.Box System Architecture" manual
 * (segaretro.org/images/7/78/DreamcastDevBoxSystemArchitecture.pdf),
 * §3.7.4.4 "Parameter Control Word", combining its three sub-fields:
 *
 * <pre>
 * Para Control (§3.7.4.4.1), HOLLY2 layout (3-bit list type incl. Punch Through):
 *   bit 31-29 : Para Type      (see {@link TaParameterType})
 *   bit 28    : End Of Strip   (valid only in Vertex Parameters)
 *   bit 27    : Reserved
 *   bit 26-24 : List Type      (see {@link TileAcceleratorListType}; valid only for
 *                                the first Global/Object-List-Set parameter after
 *                                list init or after an End Of List parameter)
 *
 * Group Control (§3.7.4.4.2), valid only in Global Parameters:
 *   bit 23    : Group_En       (must be 1 for Strip_Len/User_Clip below to apply)
 *   bit 22-20 : Reserved
 *   bit 19-18 : Strip_Len      (see {@link TaStripLength})
 *   bit 17-16 : User_Clip      (see {@link TaUserTileClipMode})
 *
 * Obj Control (§3.7.4.4.3), valid only in Global Parameters:
 *   bit 15-8  : Reserved
 *   bit 7     : Shadow
 *   bit 6     : Volume
 *   bit 5-4   : Col_Type       (see {@link TaColorType})
 *   bit 3     : Texture
 *   bit 2     : Offset
 *   bit 1     : Gouraud
 *   bit 0     : 16bit_UV
 * </pre>
 *
 * <p>This class stores every sub-field unconditionally, even though the
 * source documents several of them as only meaningful for specific
 * {@code paraType}/context combinations (e.g. {@code endOfStrip} only
 * applies to Vertex Parameters, the whole of Group/Obj Control only to
 * Global Parameters). Round-tripping the raw bits faithfully regardless
 * of context keeps this class a truthful mirror of the wire format;
 * knowing which fields are contextually meaningful is a concern for
 * whatever higher-level TA parameter parser is built on top of it.
 */
public record TaParameterControlWord(TaParameterType paraType, boolean endOfStrip,
                                      TileAcceleratorListType listType, boolean groupEnable,
                                      TaStripLength stripLength, TaUserTileClipMode userTileClip, boolean shadow,
                                      boolean volume, TaColorType colorType, boolean texture, boolean offset,
                                      boolean gouraud, boolean sixteenBitUv) {

    private static final int PARA_TYPE_SHIFT = 29;
    private static final int PARA_TYPE_MASK = 0b111;
    private static final int END_OF_STRIP_BIT = 28;
    private static final int LIST_TYPE_SHIFT = 24;
    private static final int LIST_TYPE_MASK = 0b111;
    private static final int GROUP_EN_BIT = 23;
    private static final int STRIP_LEN_SHIFT = 18;
    private static final int STRIP_LEN_MASK = 0b11;
    private static final int USER_CLIP_SHIFT = 16;
    private static final int USER_CLIP_MASK = 0b11;
    private static final int SHADOW_BIT = 7;
    private static final int VOLUME_BIT = 6;
    private static final int COL_TYPE_SHIFT = 4;
    private static final int COL_TYPE_MASK = 0b11;
    private static final int TEXTURE_BIT = 3;
    private static final int OFFSET_BIT = 2;
    private static final int GOURAUD_BIT = 1;
    private static final int UV16_BIT = 0;

    public int encode() {
        int value = (paraType.fieldValue() & PARA_TYPE_MASK) << PARA_TYPE_SHIFT;
        if (endOfStrip) {
            value |= 1 << END_OF_STRIP_BIT;
        }
        value |= (listType.fieldValue() & LIST_TYPE_MASK) << LIST_TYPE_SHIFT;
        if (groupEnable) {
            value |= 1 << GROUP_EN_BIT;
        }
        value |= (stripLength.fieldValue() & STRIP_LEN_MASK) << STRIP_LEN_SHIFT;
        value |= (userTileClip.fieldValue() & USER_CLIP_MASK) << USER_CLIP_SHIFT;
        if (shadow) {
            value |= 1 << SHADOW_BIT;
        }
        if (volume) {
            value |= 1 << VOLUME_BIT;
        }
        value |= (colorType.fieldValue() & COL_TYPE_MASK) << COL_TYPE_SHIFT;
        if (texture) {
            value |= 1 << TEXTURE_BIT;
        }
        if (offset) {
            value |= 1 << OFFSET_BIT;
        }
        if (gouraud) {
            value |= 1 << GOURAUD_BIT;
        }
        if (sixteenBitUv) {
            value |= 1 << UV16_BIT;
        }
        return value;
    }

    public static TaParameterControlWord decode(int value) {
        TaParameterType paraType = TaParameterType.fromFieldValue((value >>> PARA_TYPE_SHIFT) & PARA_TYPE_MASK);
        boolean endOfStrip = ((value >>> END_OF_STRIP_BIT) & 1) != 0;
        TileAcceleratorListType listType =
                TileAcceleratorListType.fromFieldValue((value >>> LIST_TYPE_SHIFT) & LIST_TYPE_MASK);
        boolean groupEnable = ((value >>> GROUP_EN_BIT) & 1) != 0;
        TaStripLength stripLength = TaStripLength.fromFieldValue((value >>> STRIP_LEN_SHIFT) & STRIP_LEN_MASK);
        TaUserTileClipMode userTileClip =
                TaUserTileClipMode.fromFieldValue((value >>> USER_CLIP_SHIFT) & USER_CLIP_MASK);
        boolean shadow = ((value >>> SHADOW_BIT) & 1) != 0;
        boolean volume = ((value >>> VOLUME_BIT) & 1) != 0;
        TaColorType colorType = TaColorType.fromFieldValue((value >>> COL_TYPE_SHIFT) & COL_TYPE_MASK);
        boolean texture = ((value >>> TEXTURE_BIT) & 1) != 0;
        boolean offset = ((value >>> OFFSET_BIT) & 1) != 0;
        boolean gouraud = ((value >>> GOURAUD_BIT) & 1) != 0;
        boolean sixteenBitUv = ((value >>> UV16_BIT) & 1) != 0;
        return new TaParameterControlWord(paraType, endOfStrip, listType, groupEnable, stripLength, userTileClip,
                shadow, volume, colorType, texture, offset, gouraud, sixteenBitUv);
    }
}
