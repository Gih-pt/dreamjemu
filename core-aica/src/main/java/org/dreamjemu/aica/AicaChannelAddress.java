package org.dreamjemu.aica;

/**
 * Address arithmetic for the AICA's per-channel register blocks.
 *
 * <p>Source: "Yamaha AICA Sound System Hardware Reference v0.8" by yamato
 * (hitmen.c02.at/files/docs/dc/aica_v08.txt), "Channel Registers" section:
 * the AICA registers are accessible from both the SH4 and the ARM7, at
 * base address {@code 0x00800000} (ARM7 side) / {@code 0xA0700000} (SH4
 * side). Each channel register set is 32 32-bit registers (128 bytes),
 * though only the first 18 are actually used by the hardware — "the
 * remaining 14 registers are just placeholders and simplif[y] the
 * calculation of the channel register sets", i.e. the 128-byte stride
 * itself is confirmed even though most of what's in the tail of it isn't.
 * Channel count (64) is from Marcus Comstedt's hardware overview
 * (mc.pp.se/dc/hw.html: "Yamaha AICA sound system (64 channel PCM
 * sound)").
 */
public final class AicaChannelAddress {

    /** Base address of Channel 0's registers, as seen from the ARM7 side. */
    public static final int BASE_ADDRESS_ARM7 = 0x00800000;

    /** Base address of Channel 0's registers, as seen from the SH4 side (P2 area). */
    public static final int BASE_ADDRESS_SH4 = 0xA0700000;

    /** Registers per channel block (only the first 18 are actually used by the hardware). */
    public static final int REGISTERS_PER_CHANNEL = 32;

    /** Byte distance from one channel's register block to the next. */
    public static final int CHANNEL_STRIDE_BYTES = REGISTERS_PER_CHANNEL * 4;

    public static final int CHANNEL_COUNT = 64;

    private AicaChannelAddress() {
    }

    public static int baseAddressArm7(int channel) {
        requireValidChannel(channel);
        return BASE_ADDRESS_ARM7 + channel * CHANNEL_STRIDE_BYTES;
    }

    public static int baseAddressSh4(int channel) {
        requireValidChannel(channel);
        return BASE_ADDRESS_SH4 + channel * CHANNEL_STRIDE_BYTES;
    }

    private static void requireValidChannel(int channel) {
        if (channel < 0 || channel >= CHANNEL_COUNT) {
            throw new IllegalArgumentException("channel must be 0-" + (CHANNEL_COUNT - 1) + ", got " + channel);
        }
    }
}
