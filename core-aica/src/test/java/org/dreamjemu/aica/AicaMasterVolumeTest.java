package org.dreamjemu.aica;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AicaMasterVolumeTest {

    @Test
    void encodeDecodeRoundTripsStereo() {
        AicaMasterVolume original = new AicaMasterVolume(12, false);

        AicaMasterVolume decoded = AicaMasterVolume.decode(original.encode());

        assertEquals(original, decoded);
    }

    @Test
    void encodeDecodeRoundTripsMono() {
        AicaMasterVolume original = new AicaMasterVolume(7, true);

        AicaMasterVolume decoded = AicaMasterVolume.decode(original.encode());

        assertEquals(original, decoded);
    }

    @Test
    void modeFieldMatchesDocumentedConstants() {
        assertEquals(0x0000, new AicaMasterVolume(0, false).encode() & 0xFF00);
        assertEquals(0x8000, new AicaMasterVolume(0, true).encode() & 0xFF00);
    }

    @Test
    void volumeOccupiesTheLowestFourBits() {
        AicaMasterVolume volume = new AicaMasterVolume(15, false);

        assertEquals(15, volume.encode());
    }

    @Test
    void decodeTreatsAnUndocumentedModeByteAsStereo() {
        // Only 0x00 (stereo) and 0x80 (mono) are documented; anything else
        // in the mode byte shouldn't be silently read as mono.
        int undocumentedMode = 0x40;
        AicaMasterVolume decoded = AicaMasterVolume.decode((undocumentedMode << 8) | 5);

        assertFalse(decoded.mono());
        assertEquals(5, decoded.volume());
    }

    @Test
    void documentedReadBackValueConstantMatchesTheSource() {
        assertEquals(0x10, AicaMasterVolume.DOCUMENTED_READ_BACK_VALUE);
    }

    @Test
    void rejectsVolumeOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> new AicaMasterVolume(16, false));
        assertThrows(IllegalArgumentException.class, () -> new AicaMasterVolume(-1, false));
    }
}
