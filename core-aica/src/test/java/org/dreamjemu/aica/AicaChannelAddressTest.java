package org.dreamjemu.aica;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AicaChannelAddressTest {

    @Test
    void channelZeroSitsAtTheBaseAddress() {
        assertEquals(0x00800000, AicaChannelAddress.baseAddressArm7(0));
        assertEquals(0xA0700000, AicaChannelAddress.baseAddressSh4(0));
    }

    @Test
    void eachChannelIsOneStrideFurtherThanTheLast() {
        assertEquals(0x00800080, AicaChannelAddress.baseAddressArm7(1));
        assertEquals(0x00800100, AicaChannelAddress.baseAddressArm7(2));
        assertEquals(0xA0700080, AicaChannelAddress.baseAddressSh4(1));
    }

    @Test
    void lastValidChannelIsSixtyThree() {
        int expectedOffset = 63 * AicaChannelAddress.CHANNEL_STRIDE_BYTES;
        assertEquals(AicaChannelAddress.BASE_ADDRESS_ARM7 + expectedOffset, AicaChannelAddress.baseAddressArm7(63));
    }

    @Test
    void rejectsOutOfRangeChannelNumbers() {
        assertThrows(IllegalArgumentException.class, () -> AicaChannelAddress.baseAddressArm7(-1));
        assertThrows(IllegalArgumentException.class, () -> AicaChannelAddress.baseAddressArm7(64));
        assertThrows(IllegalArgumentException.class, () -> AicaChannelAddress.baseAddressSh4(64));
    }

    @Test
    void strideMatchesThirtyTwoRegistersOfFourBytes() {
        assertEquals(128, AicaChannelAddress.CHANNEL_STRIDE_BYTES);
        assertEquals(32, AicaChannelAddress.REGISTERS_PER_CHANNEL);
    }
}
