package org.dreamjemu.aica;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AicaLoopRangeTest {

    @Test
    void encodeDecodeRoundTrips() {
        AicaLoopRange original = new AicaLoopRange(0x0100, 0x8000);

        AicaLoopRange decoded = AicaLoopRange.decode(original.encodeLoopStart(), original.encodeLoopEnd());

        assertEquals(original, decoded);
    }

    @Test
    void minimumLoopStartIsZero() {
        AicaLoopRange range = new AicaLoopRange(0x0000, 0x0001);

        assertEquals(0, range.encodeLoopStart());
        assertEquals(1, range.encodeLoopEnd());
    }

    @Test
    void maximumDocumentedValuesRoundTrip() {
        AicaLoopRange range = new AicaLoopRange(0xFFFD, 0xFFFF);

        assertEquals(range, AicaLoopRange.decode(range.encodeLoopStart(), range.encodeLoopEnd()));
    }

    @Test
    void noLoopingConventionSetsLoopEndToTheSampleLength() {
        // Per the source: "if no looping is desired this register must be set
        // to the sample length in samples before a channel is played."
        int sampleLengthInSamples = 4000;
        AicaLoopRange range = new AicaLoopRange(0, sampleLengthInSamples);

        assertEquals(sampleLengthInSamples, range.loopEndSample());
    }

    @Test
    void rejectsLoopStartBeyondItsDocumentedMaximum() {
        assertThrows(IllegalArgumentException.class, () -> new AicaLoopRange(0xFFFE, 0xFFFF));
    }

    @Test
    void rejectsLoopEndOfZero() {
        assertThrows(IllegalArgumentException.class, () -> new AicaLoopRange(0, 0));
    }

    @Test
    void rejectsLoopEndNotGreaterThanLoopStart() {
        assertThrows(IllegalArgumentException.class, () -> new AicaLoopRange(100, 100));
        assertThrows(IllegalArgumentException.class, () -> new AicaLoopRange(100, 50));
    }

    @Test
    void rejectsNegativeLoopStart() {
        assertThrows(IllegalArgumentException.class, () -> new AicaLoopRange(-1, 10));
    }
}
