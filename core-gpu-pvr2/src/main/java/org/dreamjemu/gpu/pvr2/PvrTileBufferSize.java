package org.dreamjemu.gpu.pvr2;

/**
 * The Tile Buffer size register: how many 32x32-pixel tiles wide and high
 * the tile buffer covers.
 *
 * <p>Source: "Guide to the PowerVR-chip of the Dreamcast" by Lars Olsson,
 * v0.51 (hitmen.c02.at/files/docs/dc/powervr-reg.txt), register
 * {@code a05f813c (tilebuf_size)}:
 *
 * <pre>
 * bits 31-16 : height (height of tile buffer in 32-pixel tiles, minus 1)
 * bits 15-0  : width  (width of tile buffer in 32-pixel tiles, minus 1)
 * </pre>
 *
 * <p>This class works in actual tile counts (already the "+1" of the raw
 * register field) so callers never have to remember the hardware's
 * minus-one encoding themselves; {@link #encode()}/{@link #decode} do that
 * translation at the boundary.
 */
public record PvrTileBufferSize(int heightInTiles, int widthInTiles) {

    /** P2 (uncached) address of this register. */
    public static final int REGISTER_ADDRESS = 0xA05F813C;

    private static final int MIN_TILES = 1;
    private static final int MAX_TILES = 0x10000; // a 16-bit "count minus 1" field maxes out at 65536.

    public PvrTileBufferSize {
        requireInRange("heightInTiles", heightInTiles);
        requireInRange("widthInTiles", widthInTiles);
    }

    private static void requireInRange(String name, int tiles) {
        if (tiles < MIN_TILES || tiles > MAX_TILES) {
            throw new IllegalArgumentException(name + " must be " + MIN_TILES + "-" + MAX_TILES + ", got " + tiles);
        }
    }

    /** The tile buffer's pixel dimensions (each tile is 32x32 pixels). */
    public int heightInPixels() {
        return heightInTiles * 32;
    }

    public int widthInPixels() {
        return widthInTiles * 32;
    }

    public int encode() {
        int heightField = (heightInTiles - 1) & 0xFFFF;
        int widthField = (widthInTiles - 1) & 0xFFFF;
        return (heightField << 16) | widthField;
    }

    public static PvrTileBufferSize decode(int value) {
        int height = ((value >>> 16) & 0xFFFF) + 1;
        int width = (value & 0xFFFF) + 1;
        return new PvrTileBufferSize(height, width);
    }
}
