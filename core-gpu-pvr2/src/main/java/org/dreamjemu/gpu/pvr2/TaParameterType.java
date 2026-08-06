package org.dreamjemu.gpu.pvr2;

/**
 * The {@code Para Type} field (bits 31-29) of the Parameter Control
 * Word — identifies which of the three parameter categories (Control,
 * Global, Vertex) a Tile Accelerator parameter belongs to, and which
 * specific kind within that category.
 *
 * <p>Source: Sega's "Dreamcast/Dev.Box System Architecture" manual
 * (segaretro.org/images/7/78/DreamcastDevBoxSystemArchitecture.pdf),
 * §3.7.4.4.1 "Para Control":
 *
 * <pre>
 * Parameter type      Parameter                       Hex Code
 * Control Parameter   End Of List                         0
 *                      User Tile Clip                      1
 *                      Object List Set                      2
 *                      Reserved                              3
 * Global Parameter    Polygon or Modifier Volume           4
 *                      Sprite                                5
 *                      Reserved                               6
 * Vertex Parameter                                            7
 * </pre>
 *
 * <p>The two documented "Reserved" values are kept as their own named
 * constants (rather than omitted) so {@link #fromFieldValue} round-trips
 * every legal 3-bit value without throwing on input that real hardware
 * would still accept, even if this project has nothing meaningful to do
 * with it yet.
 */
public enum TaParameterType {
    END_OF_LIST(0),
    USER_TILE_CLIP(1),
    OBJECT_LIST_SET(2),
    CONTROL_RESERVED(3),
    POLYGON_OR_MODIFIER_VOLUME(4),
    SPRITE(5),
    GLOBAL_RESERVED(6),
    VERTEX_PARAMETER(7);

    private final int fieldValue;

    TaParameterType(int fieldValue) {
        this.fieldValue = fieldValue;
    }

    public int fieldValue() {
        return fieldValue;
    }

    public static TaParameterType fromFieldValue(int fieldValue) {
        for (TaParameterType type : values()) {
            if (type.fieldValue == fieldValue) {
                return type;
            }
        }
        throw new IllegalArgumentException("Para Type field must be 0-7, got " + fieldValue);
    }
}
