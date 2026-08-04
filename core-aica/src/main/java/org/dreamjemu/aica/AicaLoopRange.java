package org.dreamjemu.aica;

/**
 * A channel's loop window, covering the {@code LoopStart} and
 * {@code LoopEnd} registers together (they only make sense as a pair —
 * the source defines {@code LoopEnd} in terms of being greater than
 * {@code LoopStart}).
 *
 * <p>Source: "Yamaha AICA Sound System Hardware Reference v0.8" by yamato
 * (hitmen.c02.at/files/docs/dc/aica_v08.txt):
 *
 * <pre>
 * 0x0008 - LoopStart
 * +---------------+
 * | 31-16 | 15-0  |
 * | n/a   | pos   |
 * +---------------+
 * pos: Set Loop Start Address in samples (0x0000 to 0xfffd).
 *
 * 0x000c - LoopEnd
 * +--------------+
 * | 31-16 | 15-0 |
 * | n/a   | pos  |
 * +--------------+
 * pos: Set Loop End Address in samples (0x0001 to 0xffff, Loop End must
 *      be &gt; Loop Start Address) if no looping is desired this register
 *      must be set to the sample length in samples before a channel is
 *      played.
 * </pre>
 */
public record AicaLoopRange(int loopStartSample, int loopEndSample) {

    /** Byte offset of the LoopStart register within a channel's register block. */
    public static final int LOOP_START_REGISTER_OFFSET = 0x0008;

    /** Byte offset of the LoopEnd register within a channel's register block. */
    public static final int LOOP_END_REGISTER_OFFSET = 0x000C;

    private static final int LOOP_START_MAX = 0xFFFD;
    private static final int LOOP_END_MIN = 0x0001;
    private static final int LOOP_END_MAX = 0xFFFF;

    public AicaLoopRange {
        if (loopStartSample < 0 || loopStartSample > LOOP_START_MAX) {
            throw new IllegalArgumentException(
                    "loopStartSample must be 0x0000-0x" + Integer.toHexString(LOOP_START_MAX) + ", got " + loopStartSample);
        }
        if (loopEndSample < LOOP_END_MIN || loopEndSample > LOOP_END_MAX) {
            throw new IllegalArgumentException(
                    "loopEndSample must be 0x0001-0xffff, got " + loopEndSample);
        }
        if (loopEndSample <= loopStartSample) {
            throw new IllegalArgumentException(
                    "loopEndSample (" + loopEndSample + ") must be greater than loopStartSample (" + loopStartSample + ")");
        }
    }

    public int encodeLoopStart() {
        return loopStartSample & 0xFFFF;
    }

    public int encodeLoopEnd() {
        return loopEndSample & 0xFFFF;
    }

    public static AicaLoopRange decode(int loopStartValue, int loopEndValue) {
        return new AicaLoopRange(loopStartValue & 0xFFFF, loopEndValue & 0xFFFF);
    }
}
