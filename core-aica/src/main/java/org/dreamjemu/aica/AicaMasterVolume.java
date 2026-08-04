package org.dreamjemu.aica;

/**
 * The global {@code MasterVolume} register — not per-channel, unlike
 * {@link AicaPlayControl}/{@link AicaLoopRange}; this sits at a fixed
 * address in the DSP output mixer register area.
 *
 * <p>Source: "Yamaha AICA Sound System Hardware Reference v0.8" by yamato
 * (hitmen.c02.at/files/docs/dc/aica_v08.txt), register
 * {@code 0x2800 - MasterVolume}:
 *
 * <pre>
 * bits 31-16 : n/a
 * bits 15-8  : mode (0x00 = stereo, 0x80 = mono)
 * bits 7-4   : n/a
 * bits 3-0   : vol (master volume of the whole sound system, 0-15)
 * </pre>
 *
 * <p>The source notes a real-hardware read quirk for this specific
 * register: <i>"this register returns always 0x10 when read"</i> —
 * i.e. a real AICA doesn't echo back whatever was last written here.
 * {@link #decode} still decodes whatever raw value it's given (so it
 * composes correctly with a future memory/register-bank model that
 * implements that read-back quirk itself), but doesn't bake the quirk in
 * — {@link #DOCUMENTED_READ_BACK_VALUE} is provided for whoever wires
 * that up later to reference and test against.
 */
public record AicaMasterVolume(int volume, boolean mono) {

    /** Address of this register (fixed, not per-channel). */
    public static final int REGISTER_ADDRESS = 0x2800;

    /** The raw value the source says a real AICA always returns when this register is read. */
    public static final int DOCUMENTED_READ_BACK_VALUE = 0x10;

    private static final int MODE_SHIFT = 8;
    private static final int MONO_MODE = 0x80;
    private static final int VOLUME_MASK = 0xF;

    public AicaMasterVolume {
        if (volume < 0 || volume > 15) {
            throw new IllegalArgumentException("volume must be 0-15, got " + volume);
        }
    }

    public int encode() {
        int value = volume & VOLUME_MASK;
        if (mono) {
            value |= MONO_MODE << MODE_SHIFT;
        }
        return value;
    }

    public static AicaMasterVolume decode(int value) {
        int mode = (value >>> MODE_SHIFT) & 0xFF;
        boolean mono = mode == MONO_MODE;
        int volume = value & VOLUME_MASK;
        return new AicaMasterVolume(volume, mono);
    }
}
