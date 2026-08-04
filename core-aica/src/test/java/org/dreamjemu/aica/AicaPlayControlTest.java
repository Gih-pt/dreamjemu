package org.dreamjemu.aica;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AicaPlayControlTest {

    @Test
    void encodeDecodeRoundTrips() {
        AicaPlayControl original = new AicaPlayControl(0b11, true, AicaSampleFormat.ADPCM_4BIT, 0x55);

        AicaPlayControl decoded = AicaPlayControl.decode(original.encode());

        assertEquals(original, decoded);
    }

    @Test
    void keyEventOccupiesBits15And14() {
        AicaPlayControl control = new AicaPlayControl(0b11, false, AicaSampleFormat.PCM_16BIT, 0);

        assertEquals(0b11 << 14, control.encode());
    }

    @Test
    void loopBitIsBit9() {
        AicaPlayControl control = new AicaPlayControl(0, true, AicaSampleFormat.PCM_16BIT, 0);

        assertEquals(1 << 9, control.encode());
    }

    @Test
    void formatOccupiesBits8And7() {
        AicaPlayControl control = new AicaPlayControl(0, false, AicaSampleFormat.ADPCM_4BIT_LOOPING, 0);

        assertEquals(0b11 << 7, control.encode());
    }

    @Test
    void addressHighOccupiesTheLowestSevenBits() {
        AicaPlayControl control = new AicaPlayControl(0, false, AicaSampleFormat.PCM_16BIT, 0x7F);

        assertEquals(0x7F, control.encode());
    }

    @Test
    void isKeyOnAndIsAftertouchReadTheOtherGuessInterpretation() {
        // keyEvent bit 0 (register bit 14) = key on per the "other guess" reading.
        AicaPlayControl keyOnOnly = new AicaPlayControl(0b01, false, AicaSampleFormat.PCM_16BIT, 0);
        assertTrue(keyOnOnly.isKeyOn());
        assertFalse(keyOnOnly.isAftertouch());

        // keyEvent bit 1 (register bit 15) = aftertouch per the "other guess" reading.
        AicaPlayControl aftertouchOnly = new AicaPlayControl(0b10, false, AicaSampleFormat.PCM_16BIT, 0);
        assertFalse(aftertouchOnly.isKeyOn());
        assertTrue(aftertouchOnly.isAftertouch());
    }

    @Test
    void rejectsKeyEventWiderThanTwoBits() {
        assertThrows(IllegalArgumentException.class,
                () -> new AicaPlayControl(0b100, false, AicaSampleFormat.PCM_16BIT, 0));
    }

    @Test
    void rejectsAddressHighWiderThanSevenBits() {
        assertThrows(IllegalArgumentException.class,
                () -> new AicaPlayControl(0, false, AicaSampleFormat.PCM_16BIT, 0x80));
    }
}
