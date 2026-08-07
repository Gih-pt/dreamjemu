package org.dreamjemu.gpu.pvr2;

/**
 * The render output modulo: how many extra 32-bit words are skipped in
 * VRAM at the end of each rendered scanline before the next one starts
 * (used when the render target's stride doesn't match its width, e.g.
 * rendering into a region of a larger texture).
 *
 * <p>Source: "Guide to the PowerVR-chip of the Dreamcast" by Lars Olsson,
 * v0.51 (hitmen.c02.at/files/docs/dc/powervr-reg.txt), register
 * {@code a05f804c (fb_render_modulo)}:
 *
 * <pre>
 * bits 31-9 : n/a
 * bits 8-0  : modulo = (bytes/pixel * width) / 8
 * </pre>
 *
 * <p>The source adds its own parenthetical caveat here — "(haven't got
 * this to work in *RGB888 modes...)" — kept verbatim in this Javadoc
 * rather than smoothed over, since it's the author's own field report of
 * a practical problem with this register in certain pixel formats, not
 * just an abstract field description.
 */
public record PvrRenderModulo(int modulo) {

    /** P2 (uncached) address of this register. */
    public static final int REGISTER_ADDRESS = 0xA05F804C;

    private static final int FIELD_MASK = 0x1FF;

    public PvrRenderModulo {
        if (modulo < 0 || modulo > FIELD_MASK) {
            throw new IllegalArgumentException("modulo must be 0-" + FIELD_MASK + ", got " + modulo);
        }
    }

    public int encode() {
        return modulo;
    }

    public static PvrRenderModulo decode(int value) {
        return new PvrRenderModulo(value & FIELD_MASK);
    }
}
